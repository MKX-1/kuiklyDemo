package com.example.aistock.backend.ai

import com.example.aistock.backend.dto.AiAnalysisDto
import com.example.aistock.backend.dto.AiFactorsDto
import com.example.aistock.backend.dto.AiProfileDto
import com.example.aistock.backend.dto.AiSummaryDto
import com.example.aistock.backend.dto.StockItemDto
import kotlin.math.roundToLong

/**
 * AI 分析引擎（接缝）。
 *
 * 现在只有规则引擎一个实现；将来接真大模型时，新增一个 `LlmAiEngine : AiEngine` 即可，
 * 路由层和客户端都不用改——这就是"面向接口 + 可插拔"的价值。
 */
interface AiEngine {
    fun profile(item: StockItemDto): AiProfileDto
    suspend fun analysis(item: StockItemDto): AiAnalysisDto
    fun summary(stocks: List<StockItemDto>, generatedAt: Long): AiSummaryDto
    fun tags(item: StockItemDto): List<String>
}

/** 画像取值与阈值的单一事实源。⚠️ 必须与客户端 `AiLabels` / `AiThresh` 保持逐条一致。 */
object AiLabels {
    const val ACTION_FOCUS = "重点关注"
    const val ACTION_DIP = "低吸关注"
    const val ACTION_HOLD = "持股观望"
    const val ACTION_AVOID = "建议回避"

    const val SIGNAL_VOLUME = "量能放大"
    const val SIGNAL_MACD = "MACD金叉"
    const val SIGNAL_BOTTOM = "低位企稳"
    const val SIGNAL_OVERSOLD = "超跌反弹"

    const val SCENARIO_ADD = "建议加自选"
    const val SCENARIO_BUILD = "建议建仓"
    const val SCENARIO_KEEP = "继续持有"
    const val SCENARIO_CUT = "建议减仓"
}

object AiThresh {
    const val SCORE_HIGH = 85
    const val SCORE_MID = 70
    const val ACTION_FOCUS_PCT = 3.0
    const val ACTION_DIP_PCT = 1.0
    const val ACTION_AVOID_PCT = -1.0
    const val SIGNAL_SURGE_PCT = 2.0
    const val PE_GOOD = 20.0
    const val PE_BAD = 40.0
    const val TARGET_RATIO = 0.08
    const val STOP_RATIO = 0.05
    const val TAG_VOLUME_RATIO = 1.5
    const val TAG_AMPLITUDE_PCT = 4.0
    const val TAG_LEAD_PCT = 3.0
}

/**
 * 规则引擎实现。
 *
 * ⚠️ 说清楚定位：这是**演示用的规则引擎，不是真正的 AI 分析**。
 * 它只用「当日涨跌幅 + 市盈率」做机械推导，输出结构化的评分与依据。
 * 接真 LLM 后，这些字段由模型产出，本类退化为兜底。
 */
object RuleEngineAiEngine : AiEngine {

    override fun profile(item: StockItemDto): AiProfileDto {
        val pct = item.changePct
        val pe = item.pe

        val action = when {
            pct > AiThresh.ACTION_FOCUS_PCT -> AiLabels.ACTION_FOCUS
            pct in AiThresh.ACTION_DIP_PCT..AiThresh.ACTION_FOCUS_PCT -> AiLabels.ACTION_DIP
            pct > AiThresh.ACTION_AVOID_PCT -> AiLabels.ACTION_HOLD
            else -> AiLabels.ACTION_AVOID
        }

        val signal = when {
            pct > AiThresh.SIGNAL_SURGE_PCT && pe <= 0 -> AiLabels.SIGNAL_VOLUME
            pct > AiThresh.SIGNAL_SURGE_PCT -> AiLabels.SIGNAL_MACD
            pe in 1.0..AiThresh.PE_GOOD -> AiLabels.SIGNAL_BOTTOM
            else -> AiLabels.SIGNAL_OVERSOLD
        }

        val (momentum, value, risk) = scoreParts(pct, pe)
        val score = (50 + momentum + value + risk).coerceIn(0, 100)

        val scenario = when {
            action == AiLabels.ACTION_FOCUS && score >= AiThresh.SCORE_HIGH -> AiLabels.SCENARIO_ADD
            action == AiLabels.ACTION_DIP -> AiLabels.SCENARIO_BUILD
            action == AiLabels.ACTION_HOLD -> AiLabels.SCENARIO_KEEP
            else -> AiLabels.SCENARIO_CUT
        }
        return AiProfileDto(action, signal, score, scenario)
    }

    override suspend fun analysis(item: StockItemDto): AiAnalysisDto {
        val p = item.aiProfile
        val pct = item.changePct
        val (momentum, value, risk) = scoreParts(pct, item.pe)

        val trendLabel = when {
            pct > AiThresh.ACTION_FOCUS_PCT -> "短期看涨信号明显"
            pct > 0.0 -> "短期震荡偏强"
            pct > -2.0 -> "短期窄幅整理"
            else -> "短期承压回落"
        }
        val riskLevel = when {
            p.score >= AiThresh.SCORE_HIGH -> "低风险"
            p.score >= AiThresh.SCORE_MID -> "中低风险"
            else -> "中高风险"
        }
        return AiAnalysisDto(
            code = item.code,
            name = item.name,
            trendLabel = trendLabel,
            trendText = "驱动${p.action}（${p.score}分），${p.signal}；今日涨跌幅 ${pct}%。",
            riskLevel = riskLevel,
            riskText = "当前评级：$riskLevel",
            score = p.score,
            targetPrice = (item.price + item.price * AiThresh.TARGET_RATIO).roundToLong(),
            stopLossPrice = (item.price - item.price * AiThresh.STOP_RATIO).roundToLong(),
            factors = AiFactorsDto(
                momentum = momentum,
                value = value,
                risk = risk,
                industry = item.industry,
                benchmarkDelta = item.benchmarkDelta,
            ),
        )
    }

    override fun summary(stocks: List<StockItemDto>, generatedAt: Long): AiSummaryDto {
        if (stocks.isEmpty()) return AiSummaryDto("暂无行情数据", generatedAt)
        val up = stocks.count { it.changePct > 0 }
        val down = stocks.count { it.changePct < 0 }
        val best = stocks.maxByOrNull { it.changePct }
        val worst = stocks.minByOrNull { it.changePct }
        val avg = stocks.map { it.changePct }.average()
        val tone = if (avg >= 0) "今日整体偏强，关注领涨股" else "今日偏弱，注意控制仓位"
        val text = "自选 ${up} 涨 ${down} 跌；最强「${best?.name}」${best?.changePct}%，" +
            "最弱「${worst?.name}」${worst?.changePct}%。$tone。"
        return AiSummaryDto(text, generatedAt, stale = false)
    }

    override fun tags(item: StockItemDto): List<String> {
        val tags = mutableListOf<String>()
        item.benchmarkDelta?.let { if (it > 0) tags += "强于大盘" }
        if (item.volumeRatio >= AiThresh.TAG_VOLUME_RATIO) {
            tags += "量比异动"
            if (item.changePct > 0) tags += "放量上攻"
        }
        if (item.amplitude >= AiThresh.TAG_AMPLITUDE_PCT) tags += "振幅放大"
        if (item.changePct >= AiThresh.TAG_LEAD_PCT) tags += "领涨"
        if (item.pe in 1.0..AiThresh.PE_GOOD && item.changePct < 0) tags += "低位企稳"
        return tags
    }

    private fun scoreParts(changePct: Double, pe: Double): Triple<Int, Int, Int> = Triple(
        (changePct.coerceIn(-5.0, 8.0) / 8.0 * 60.0).toInt(),
        if (pe in 1.0..AiThresh.PE_GOOD) 25 else if (pe > AiThresh.PE_BAD) 10 else 15,
        when {
            changePct < -2.0 -> 0
            changePct < 0.0 -> 5
            else -> 8
        },
    )
}

/** 静态行业表（演示用）。真实项目应换成行业数据库或上游接口。 */
object IndustryMap {
    const val UNCLASSIFIED = "未分类"

    private val byCode = mapOf(
        "600519" to "白酒",
        "000858" to "白酒",
        "00700" to "互联网",
        "300750" to "电池",
        "002594" to "汽车",
        "601318" to "保险",
        "688981" to "半导体",
    )

    fun of(code: String): String = byCode[code] ?: UNCLASSIFIED
}
