package com.example.aistock.backend.ai

import com.example.aistock.backend.client.httpPostJson
import com.example.aistock.backend.config.Config
import com.example.aistock.backend.dto.AiAnalysisDto
import com.example.aistock.backend.dto.AiFactorsDto
import com.example.aistock.backend.dto.AiProfileDto
import com.example.aistock.backend.dto.AiSummaryDto
import com.example.aistock.backend.dto.StockItemDto
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import java.io.IOException

/**
 * 真 LLM 分析引擎（阿里云百炼，OpenAI 兼容协议）。
 *
 * **LLM 优先、规则兜底**的分层策略：
 *  - [analysis]（弹层/报告页的深度分析）走 LLM——低频调用，秒级延时可接受；
 *  - [profile]/[tags]/[summary]（列表页高频调用）直接委托规则引擎——不值得为它们等 LLM。
 *
 * 防幻觉三道闸：
 *  1. prompt 里只喂**当日真实行情因子**，并明确告知「没有的数据不许编」；
 *  2. action/riskLevel 限定枚举，score/因子给数值区间，越界一律钳制；
 *  3. 任何异常（超时/网络/JSON 解析失败）→ 静默降级到规则引擎结果，来源如实标 rules。
 *
 * 目标价/止损价**不让 LLM 参与**（模型算价格数字不可靠），沿用规则的 ±8%/±5% 口径。
 */
class LlmAiEngine(
    private val config: Config,
    private val fallback: AiEngine,
) : AiEngine {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // 高频接口直接委托规则引擎
    override fun profile(item: StockItemDto): AiProfileDto = fallback.profile(item)
    override fun tags(item: StockItemDto): List<String> = fallback.tags(item)
    override fun summary(stocks: List<StockItemDto>, generatedAt: Long): AiSummaryDto =
        fallback.summary(stocks, generatedAt)

    override suspend fun analysis(item: StockItemDto): AiAnalysisDto {
        val base = fallback.analysis(item)
        if (config.llmApiKey.isBlank()) return base

        return try {
            val llm = withTimeout(config.llmTimeoutMs) { callLlm(item) } ?: return base.copy(source = "rules")
            val parsed = parseLlmJson(llm) ?: run {
                println("[LlmAiEngine] JSON 解析失败，降级规则引擎。原文: ${llm.take(300)}")
                return base.copy(source = "rules")
            }
            // 数值钳制：LLM 的数字只能信在区间内的部分
            val score = (parsed.score.takeIf { it > 0 } ?: base.score).coerceIn(0, 100)
            val momentum = (parsed.factors?.momentum ?: base.factors.momentum).coerceIn(0, 60)
            val value = (parsed.factors?.value ?: base.factors.value).coerceIn(0, 25)
            val risk = (parsed.factors?.risk ?: base.factors.risk).coerceIn(0, 8)
            val action = parsed.action.takeIf { it in KNOWN_ACTIONS } ?: item.aiProfile.action
            val riskLevel = parsed.riskLevel.takeIf { it in KNOWN_RISK } ?: base.riskLevel

            AiAnalysisDto(
                code = base.code,
                name = base.name,
                trendLabel = parsed.trendLabel.ifBlank { base.trendLabel },
                trendText = parsed.trendText.ifBlank { base.trendText },
                riskLevel = riskLevel,
                riskText = parsed.riskText.ifBlank { base.riskText },
                score = score,
                // 价格口径不让 LLM 碰
                targetPrice = base.targetPrice,
                stopLossPrice = base.stopLossPrice,
                factors = base.factors.copy(momentum = momentum, value = value, risk = risk),
                source = "llm",
            )
        } catch (e: TimeoutCancellationException) {
            println("[LlmAiEngine] 超时(${config.llmTimeoutMs}ms)，降级规则引擎")
            base.copy(source = "rules")
        } catch (e: IOException) {
            println("[LlmAiEngine] 网络失败: ${e.message}，降级规则引擎")
            base.copy(source = "rules")
        } catch (e: Exception) {
            println("[LlmAiEngine] 未预期异常: ${e.javaClass.simpleName}: ${e.message}，降级规则引擎")
            base.copy(source = "rules")
        }
    }

    /** 调百炼 chat/completions，返回首个 choice 的文本内容。 */
    private suspend fun callLlm(item: StockItemDto): String? {
        val system = """
你是股票分析助手。基于用户给的**当日行情数据**输出分析结论，规则：
1. 只使用提供的数据，数据里没有的（如资金流、新闻、近5日走势）一律不得提及或暗示。
2. 不构成投资建议的措辞不用写（调用方统一加免责声明）。
3. action 只能取：重点关注 / 低吸关注 / 持股观望 / 建议回避。
4. riskLevel 只能取：低风险 / 中低风险 / 中高风险。
5. score 为 0-100 整数综合评分；factors.momentum 0-60、factors.value 0-25、factors.risk 0-8。
6. trendText 与 riskText 各写 1-2 句完整中文（每句以句号结尾）。
7. 严格只输出一个 JSON 对象，不要 markdown 代码块，不要任何解释文字。
JSON 格式：
{"score":80,"action":"持股观望","signal":"量价信号短语","trendLabel":"一句话趋势标签","trendText":"趋势详述","riskLevel":"中低风险","riskText":"风险详述","factors":{"momentum":30,"value":20,"risk":6}}
        """.trimIndent()

        val user = buildString {
            append("股票：${item.name}（${item.code}，行业：${item.industry}）").append('\n')
            append("现价：${item.price} 分（注意单位是分）").append('\n')
            append("今日涨跌幅：${item.changePct}%；振幅：${item.amplitude}%；换手率：${item.turnover}%").append('\n')
            if (item.volumeRatio > 0) append("量比：${item.volumeRatio}").append('\n')
            if (item.pe > 0) append("市盈率：${item.pe}").append('\n')
            item.benchmarkDelta?.let { append("相对大盘：${it}%").append('\n') }
            if (item.marketCap > 0) append("总市值：${item.marketCap} 元").append('\n')
        }

        // Map<String, Any> 无法被 kotlinx 序列化（Any 没有serializer）——用 JsonObject 构建器
        val body = kotlinx.serialization.json.buildJsonObject {
            put("model", config.llmModel)
            put("temperature", 0.4)
            put("messages", kotlinx.serialization.json.buildJsonArray {
                add(kotlinx.serialization.json.buildJsonObject {
                    put("role", "system")
                    put("content", system)
                })
                add(kotlinx.serialization.json.buildJsonObject {
                    put("role", "user")
                    put("content", user)
                })
            })
        }.toString()

        val resp = httpPostJson(
            url = LLM_URL,
            bodyJson = body,
            timeoutMs = config.llmTimeoutMs,
            headers = mapOf("Authorization" to "Bearer " + config.llmApiKey),
        )

        // 提取 choices[0].message.content
        val root = json.parseToJsonElement(resp).jsonObject
        val content = (root["choices"] as? kotlinx.serialization.json.JsonArray)
            ?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.contentOrNull
        return content
    }

    /** 从模型回复中抠出第一个 {...} 并解析（模型偶尔会带 markdown 围栏）。 */
    private fun parseLlmJson(text: String): LlmAnalysis? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching {
            json.decodeFromString(LlmAnalysis.serializer(), text.substring(start, end + 1))
        }.getOrNull()
    }

    companion object {
        const val LLM_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        private val KNOWN_ACTIONS = setOf("重点关注", "低吸关注", "持股观望", "建议回避")
        private val KNOWN_RISK = setOf("低风险", "中低风险", "中高风险")
    }
}

/** LLM 输出契约（可空字段一律容错，钳制在引擎层做）。 */
@kotlinx.serialization.Serializable
data class LlmAnalysis(
    val score: Int = 50,
    val action: String = "",
    val signal: String = "",
    val trendLabel: String = "",
    val trendText: String = "",
    val riskLevel: String = "",
    val riskText: String = "",
    val factors: LlmFactors? = null,
)

@kotlinx.serialization.Serializable
data class LlmFactors(
    val momentum: Int = 0,
    val value: Int = 0,
    val risk: Int = 0,
)
