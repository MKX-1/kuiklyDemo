package com.example.aistock.data

import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToLong

/**
 * 腾讯行情「~ 分隔协议」解析器。纯函数，无副作用。
 *
 * 协议形态：`v_sh600519="1~贵州茅台~600519~1856.00~...~"`，字段用 `~` 分隔。
 * 下面这些位号是对着真实响应核对过的，**不要凭猜**——历史上踩过的坑：
 *  - 21..24 是买卖五档，不是涨跌数据；
 *  - 32..35 看着像涨跌幅/最高/最低，实际会错位到复合字段；
 *  - PE 在 A 股和港股都是 39 位，40 位恒为空。
 *
 * 单位换算：价格是「元」→ 存「分」（×100）；市值是「亿」→ 存「元」（×1e8）。
 */
internal object TencentQuoteParser {

    const val NAME = 1
    const val CODE = 2
    const val PRICE = 3
    const val OPEN = 5
    const val CHANGE = 31
    const val CHANGE_PCT = 32
    const val HIGH = 33
    const val LOW = 34
    const val PE = 39
    const val AMPLITUDE = 43
    const val FLOAT_CAP = 44        // 亿
    const val MARKET_CAP = 45       // 亿
    const val AMOUNT = 37           // A 股单位「万」，港股是「元」
    const val TURNOVER_A = 38
    const val TURNOVER_HK = 59
    const val VOLUME_RATIO_A = 49   // 港股该位是 52 周区间，不是量比

    data class Quote(
        val code: String,
        val name: String,
        val price: Long,
        val change: Long,
        val changePct: Double,
        val open: Long,
        val high: Long,
        val low: Long,
        val marketCap: Long,
        val floatCap: Long,
        val pe: Double,
        val turnover: Double,
        val volumeRatio: Double,
        val amplitude: Double,
        val amount: Long,
    )

    /** 从整体响应里抠出 `v_<token>="…"` 引号内的内容。 */
    fun extractBody(raw: String?, token: String): String? {
        if (raw == null) return null
        val key = "v_$token="
        val at = raw.indexOf(key)
        if (at < 0) return null
        val start = raw.indexOf('"', at)
        if (start < 0) return null
        val end = raw.indexOf('"', start + 1)
        if (end < 0) return null
        return raw.substring(start + 1, end)
    }

    /** 只取涨跌幅（给指数用，指数不需要其它字段）。 */
    fun changePctOnly(body: String?): Double? {
        if (body == null) return null
        val f = body.split("~")
        if (f.size <= CHANGE_PCT) return null
        return f[CHANGE_PCT].toDoubleOrNull()
    }

    /** 解析单只股票的完整行情；字段不够则返回 null（调用方计为 missing）。 */
    fun parse(body: String?, market: String): Quote? {
        if (body == null) return null
        val f = body.split("~")
        if (f.size <= LOW) return null
        fun at(i: Int): String = if (i < f.size) f[i] else ""
        return Quote(
            code = at(CODE),
            name = at(NAME),
            price = yuanToFen(at(PRICE)),
            change = yuanToFen(at(CHANGE)),
            changePct = at(CHANGE_PCT).toDoubleOrNull() ?: 0.0,
            open = yuanToFen(at(OPEN)),
            high = yuanToFen(at(HIGH)),
            low = yuanToFen(at(LOW)),
            marketCap = yiToYuan(at(MARKET_CAP)),
            floatCap = yiToYuan(at(FLOAT_CAP)),
            pe = at(PE).toDoubleOrNull() ?: 0.0,
            turnover = at(if (market == "hk") TURNOVER_HK else TURNOVER_A).toDoubleOrNull() ?: 0.0,
            // 港股该位不是量比 → 直接给 0，标签规则才不会假触发
            volumeRatio = if (market == "hk") 0.0 else (at(VOLUME_RATIO_A).toDoubleOrNull() ?: 0.0),
            amplitude = at(AMPLITUDE).toDoubleOrNull() ?: 0.0,
            amount = if (market == "hk") {
                at(AMOUNT).toDoubleOrNull()?.roundToLong() ?: 0L
            } else {
                at(AMOUNT).toDoubleOrNull()?.let { (it * 10_000.0).roundToLong() } ?: 0L
            },
        )
    }

    private fun yuanToFen(s: String): Long = s.toDoubleOrNull()?.let { (it * 100.0).roundToLong() } ?: 0L

    private fun yiToYuan(s: String): Long = s.toDoubleOrNull()?.let { (it * 100_000_000.0).roundToLong() } ?: 0L
}

/**
 * 直连腾讯行情（降级链的第二级）。
 *
 * 走框架自带的 [NetworkModule]，不需要引入任何 HTTP 客户端库——
 * 网络能力由 Kuikly 的宿主层提供，业务代码保持跨端。
 *
 * 这一级存在的意义：自建后端没起（开发期很常见）时，App 依然能显示真实行情。
 */
class TencentStockApi(private val network: () -> NetworkModule) : StockApi {

    override suspend fun fetchWatchlist(): WatchlistBundle {
        // 真实大盘指数：与个股同一次请求取回，绝不硬编码指数涨跌幅
        val markets = Watchlist.tokens.map { Watchlist.marketOf(it) }.distinct()
        val indexTokens = markets.map { BenchmarkIndex.indexTokenFor(it) }
        val query = (Watchlist.tokens + indexTokens).joinToString(",")

        val raw = requestRaw("http://qt.gtimg.cn/q=$query")
        if (raw == null) {
            // 整体网络失败：返回空表，由路由层继续降级到离线样例
            return WatchlistBundle(emptyList(), nowMillis(), DataSource.OFFLINE, missing = Watchlist.tokens.size)
        }

        // 市场 → 指数涨跌幅
        val indexPct = mutableMapOf<String, Double>()
        markets.forEach { market ->
            val body = TencentQuoteParser.extractBody(raw, BenchmarkIndex.indexTokenFor(market))
            TencentQuoteParser.changePctOnly(body)?.let { indexPct[market] = it }
        }

        val items = mutableListOf<StockItem>()
        var missing = 0
        for (token in Watchlist.tokens) {
            val market = Watchlist.marketOf(token)
            val quote = TencentQuoteParser.parse(TencentQuoteParser.extractBody(raw, token), market)
            if (quote == null) {
                missing++
                continue
            }
            val base = StockItem(
                id = token,
                code = quote.code.ifBlank { Watchlist.codeOf(token) },
                // 名字优先用上游返回的；但上游是 GBK 编码，某些平台的网络层会按 UTF-8 解，
                // 解出来是一串 \uFFFD（乱码）——这时必须回落到本地名字表，而不是把乱码显示给用户。
                name = quote.name.takeIf { it.isNotBlank() && !it.contains('\uFFFD') }
                    ?: Watchlist.fallbackName(token),
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
                amplitude = quote.amplitude,
                amount = quote.amount,
                // 上游协议里没有行业字段（那是后端补的），直连模式下用本地静态表兜底，
                // 否则界面上所有股票都会显示「未分类」
                industry = Watchlist.industryOf(quote.code.ifBlank { Watchlist.codeOf(token) }),
            )
            val withProfile = base.copy(aiProfile = deriveAiProfile(base))
            items.add(enrich(withProfile, indexPct[market]))
        }

        val now = nowMillis()
        return WatchlistBundle(
            stocks = items,
            fetchedAt = now,
            source = DataSource.LIVE,
            summary = deriveSummary(items, now),
            missing = missing,
        )
    }

    override suspend fun fetchStock(code: String): StockItem? =
        fetchWatchlist().stocks.firstOrNull { it.code == code }

    override suspend fun fetchAiAnalysis(code: String): AiAnalysis {
        val item = fetchStock(code) ?: SampleStockApi.fetchStock(code) ?: return SampleStockApi.fetchAiAnalysis(code)
        return deriveAiAnalysis(item)
    }

    /**
     * 走势：这一级**刻意不提供**。
     *
     * 腾讯的分时数据是一段"字符串形式的数组字面量"，客户端解析它等于把那套脏格式搬进 App，
     * 与「客户端零解析」的原则冲突。所以直连分支下详情页不显示走势图（而不是画一条假的）。
     */
    override suspend fun fetchChart(token: String): ChartSeries? = null

    /**
     * 取原始文本。
     *
     * Kuikly 的 NetworkModule 对非 JSON 回包会包一层 `{"data": "<原文>"}`，
     * 所以这里读 `data` 字段拿原文，而不是把回包当 JSON 解析。
     */
    private suspend fun requestRaw(url: String): String? = suspendCoroutine { cont ->
        val nm = network()
        nm.requestGet(url, JSONObject()) { data, success, _, _ ->
            cont.resume(if (success) data.optString("data").takeIf { it.isNotEmpty() } else null)
        }
    }
}
