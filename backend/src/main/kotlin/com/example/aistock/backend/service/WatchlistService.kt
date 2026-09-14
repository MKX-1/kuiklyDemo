package com.example.aistock.backend.service

import com.example.aistock.backend.ai.AiEngine
import com.example.aistock.backend.ai.IndustryMap
import com.example.aistock.backend.client.TencentClient
import com.example.aistock.backend.dto.AiAnalysisDto
import com.example.aistock.backend.dto.AiProfileDto
import com.example.aistock.backend.dto.StockItemDto
import com.example.aistock.backend.dto.WatchlistResponseDto
import com.example.aistock.backend.model.RawStock
import com.example.aistock.backend.parse.QuoteParser

/** 上游（腾讯）不可用。由路由层翻译成 502，避免把上游细节泄漏成 500。 */
class UpstreamUnavailable(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * 自选行情服务：编排「取数 → 解析 → 富化 → AI 打分 → 组装契约」。
 *
 * 分层意图：Service 只做流程编排，解析在 [QuoteParser]、打分在 [AiEngine]、
 * 取数在 [TencentClient]。每层单独可测，也单独可换。
 */
class WatchlistService(
    private val client: TencentClient,
    private val ai: AiEngine,
) {

    /** 按客户端传来的 codes 拉取（codes 形如 "sh600519,hk00700"）。 */
    suspend fun fetchWatchlist(codes: String): WatchlistResponseDto {
        val tokens = codes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return WatchlistResponseDto(emptyList(), 0)

        // 顺带取大盘指数，用来算「个股相对大盘强弱」——指数是真拉的，不硬编码
        val markets = tokens.map { marketOf(it) }.distinct()
        val indexTokens = markets.map { indexTokenFor(it) }
        val raw = fetchOrThrow((tokens + indexTokens).joinToString(","))

        val indexPct = mutableMapOf<String, Double>()
        markets.forEach { market ->
            QuoteParser.changePctOnly(QuoteParser.extractBody(raw, indexTokenFor(market)))
                ?.let { indexPct[market] = it }
        }

        val items = mutableListOf<StockItemDto>()
        var missing = 0
        for (token in tokens) {
            val quote = QuoteParser.parse(QuoteParser.extractBody(raw, token), marketOf(token))
            if (quote == null) {
                missing++          // 单只失败不算整体失败，但要如实计数返回给客户端
                continue
            }
            items += toItemDto(quote, indexPct[marketOf(token)])
        }

        val now = System.currentTimeMillis()
        return WatchlistResponseDto(stocks = items, missing = missing, summary = ai.summary(items, now))
    }

    /** 单只股票的分析结论。找不到返回 null → 路由层给 404。 */
    suspend fun fetchAnalysis(token: String): AiAnalysisDto? {
        val market = marketOf(token)
        val indexToken = indexTokenFor(market)
        // 连指数一起取：否则同一只票在列表页有「强于大盘」、在分析页是「—」，两处数值打架
        val raw = fetchOrThrow("$token,$indexToken")
        val indexPct = QuoteParser.changePctOnly(QuoteParser.extractBody(raw, indexToken))
        val quote = QuoteParser.parse(QuoteParser.extractBody(raw, token), market) ?: return null
        return ai.analysis(toItemDto(quote, indexPct))
    }

    private suspend fun fetchOrThrow(query: String): String = try {
        client.fetch(query)
    } catch (e: Throwable) {
        throw UpstreamUnavailable("tencent quote fetch failed: ${e.message}", e)
    }

    /** 原始行情 → 对外契约（补行业、基准差、振幅、标签、画像）。 */
    private fun toItemDto(quote: RawStock, indexChangePct: Double?): StockItemDto {
        val industry = IndustryMap.of(quote.code)
        val benchmarkDelta = indexChangePct?.let { quote.changePct - it }
        val base = StockItemDto(
            code = quote.code,
            name = quote.name,
            price = quote.price,
            change = quote.change,
            changePct = quote.changePct,
            open = quote.open,
            high = quote.high,
            low = quote.low,
            marketCap = quote.marketCap,
            floatCap = quote.floatCap,
            pe = quote.pe,
            turnover = quote.turnover,
            volumeRatio = quote.volumeRatio,
            amplitude = deriveAmplitude(quote),
            amount = quote.amount,
            industry = industry,
            benchmarkDelta = benchmarkDelta,
            aiProfile = AiProfileDto("", "", 0, ""),   // 占位，紧接着重填
        )
        val withProfile = base.copy(aiProfile = ai.profile(base))
        return withProfile.copy(tags = ai.tags(withProfile))
    }

    /**
     * 振幅兜底：上游部分市场不给振幅字段。
     * 缺了会显示成「振幅 0.00%」，那是**假信息**——0 表示"没有波动"，与"没数据"完全不同。
     * 用「(最高−最低)/昨收」自己算，昨收 = 现价 − 涨跌额。
     */
    private fun deriveAmplitude(quote: RawStock): Double {
        if (quote.amplitude > 0.0) return quote.amplitude
        val prevClose = quote.price - quote.change
        if (prevClose <= 0L || quote.high <= 0L || quote.low <= 0L) return 0.0
        return (quote.high - quote.low).toDouble() / prevClose * 100.0
    }

    private fun marketOf(token: String): String = if (token.startsWith("hk")) "hk" else token.substring(0, 2)

    private fun indexTokenFor(market: String): String = when (market) {
        "sh" -> "sh000001"
        "sz" -> "sz399001"
        else -> "hkHSI"
    }
}
