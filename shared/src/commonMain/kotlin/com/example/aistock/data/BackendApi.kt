package com.example.aistock.data

import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 自建后端的接入配置。
 *
 * **不同端访问宿主机的方式不一样**，所以不写死一个地址，而是按候选顺序探活 `/health`，命中即缓存：
 *  - 真机通过 `adb reverse tcp:8080 tcp:8080` 后，`127.0.0.1` 直接可达（放第一位，秒连）；
 *  - Android 模拟器里 `127.0.0.1` 是模拟器自己，需落到 `10.0.2.2`（宿主机别名）；
 *  - 走局域网时把 `http://<你的电脑IP>:8080` 插到第一位即可。
 */
object BackendConfig {
    var hostCandidates: List<String> = listOf(
        "http://127.0.0.1:8080",
        "http://10.0.2.2:8080",
    )

    /** 探活超时（秒）。故意短：后端没起时不拖慢首屏。 */
    var probeTimeoutSec: Int = 3

    /** 业务请求超时（秒）。 */
    var requestTimeoutSec: Int = 10

    internal var resolvedBaseUrl: String? = null
    internal var probed: Boolean = false
    internal val probeLock = Mutex()

    fun reset() {
        resolvedBaseUrl = null
        probed = false
    }
}

/**
 * 后端 JSON 取数。
 *
 * 不可达 / 非 2xx / 解析失败一律返回 null —— **这里不做任何兜底**，
 * 兜底是 [RoutedStockApi] 的职责。分开的好处是：「后端挂了」和「后端答了但没这只票」
 * 在路由层是可区分的，页面才敢如实说「当前展示的是示例数据」。
 */
internal class BackendHttp(private val network: () -> NetworkModule) {

    suspend fun getJson(path: String): JSONObject? {
        val base = baseUrl() ?: return null
        return request(base + path, BackendConfig.requestTimeoutSec)
    }

    /** 按候选顺序探活，首个可用者胜出并缓存；全失败则记 null（进程内不再重探）。 */
    private suspend fun baseUrl(): String? = BackendConfig.probeLock.withLock {
        if (BackendConfig.probed) return@withLock BackendConfig.resolvedBaseUrl
        var found: String? = null
        for (candidate in BackendConfig.hostCandidates) {
            if (request(candidate + "/health", BackendConfig.probeTimeoutSec) != null) {
                found = candidate
                break
            }
        }
        BackendConfig.resolvedBaseUrl = found
        BackendConfig.probed = true
        found
    }

    private suspend fun request(url: String, timeoutSec: Int): JSONObject? = suspendCoroutine { cont ->
        network().httpRequest(
            url = url,
            isPost = false,
            param = JSONObject(),
            headers = null,
            cookie = null,
            timeout = timeoutSec,
        ) { data, success, _, _ ->
            cont.resume(if (success) data else null)
        }
    }
}

/**
 * 后端这一级数据源。客户端零解析：腾讯协议的位号、GBK 名称、行业、标签、画像全部由后端算好，
 * 这里只做「JSON → 领域模型」的字段搬运。
 */
class BackendApi(private val network: () -> NetworkModule) {

    private val http = BackendHttp(network)

    suspend fun watchlist(): WatchlistBundle? {
        val json = http.getJson("/watchlist?codes=${Watchlist.query}") ?: return null
        val stocks = json.optJSONArray("stocks").toStocks()
        if (stocks.isEmpty()) return null
        return WatchlistBundle(
            stocks = stocks,
            fetchedAt = nowMillis(),
            source = DataSource.LIVE,
            summary = json.optJSONObject("summary").toSummary(),
            missing = json.optInt("missing"),
        )
    }

    suspend fun analysis(code: String): AiAnalysis? {
        val token = Watchlist.tokenOf(code) ?: return null
        return http.getJson("/analysis/$token")?.toAnalysis()
    }

    /** 走势（分时 / K 线）。上游那套脏格式由后端消化，这里只搬字段、按 period 分派。 */
    suspend fun chart(token: String, period: String): ChartData? {
        val json = http.getJson("/chart?token=$token&period=$period") ?: return null
        return when (period) {
            ChartPeriods.MINUTE -> {
                val points = json.optJSONArray("points").toChartPoints()
                if (points.isEmpty()) return null
                ChartData.Minute(
                    ChartSeries(
                        token = json.optString("token").ifEmpty { token },
                        code = json.optString("code"),
                        name = json.optString("name"),
                        period = json.optString("period").ifEmpty { ChartPeriods.MINUTE },
                        date = json.optString("date"),
                        prevClose = json.optLong("prevClose"),
                        points = points,
                    ),
                )
            }
            else -> {
                val candles = json.optJSONArray("candles").toCandles()
                if (candles.isEmpty()) return null
                ChartData.Kline(
                    KlineSeries(
                        token = json.optString("token").ifEmpty { token },
                        code = json.optString("code"),
                        name = json.optString("name"),
                        period = json.optString("period").ifEmpty { period },
                        prevClose = json.optLong("prevClose"),
                        candles = candles,
                    ),
                )
            }
        }
    }
}

private fun JSONArray?.toChartPoints(): List<ChartPoint> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i ->
        val o = optJSONObject(i) ?: return@mapNotNull null
        val price = o.optLong("price")
        if (price <= 0L) return@mapNotNull null
        ChartPoint(
            time = o.optString("time"),
            price = price,
            avg = o.optLong("avg"),
            volume = o.optLong("volume"),
        )
    }
}

private fun JSONArray?.toCandles(): List<Candle> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i ->
        val o = optJSONObject(i) ?: return@mapNotNull null
        val open = o.optLong("open")
        val close = o.optLong("close")
        if (open <= 0L || close <= 0L) return@mapNotNull null
        Candle(
            time = o.optString("time"),
            open = open,
            close = close,
            high = o.optLong("high"),
            low = o.optLong("low"),
            volume = o.optLong("volume"),
        )
    }
}

private fun JSONArray?.toStocks(): List<StockItem> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i -> optJSONObject(i)?.toStockItem() }
}

private fun JSONObject.toStockItem(): StockItem? {
    val code = optString("code")
    if (code.isEmpty()) return null
    val profile = optJSONObject("aiProfile")
    return StockItem(
        id = code,
        code = code,
        name = optString("name"),
        price = optLong("price"),
        change = optLong("change"),
        changePct = optDouble("changePct", 0.0),
        open = optLong("open"),
        high = optLong("high"),
        low = optLong("low"),
        marketCap = optLong("marketCap"),
        floatCap = optLong("floatCap"),
        pe = optDouble("pe", 0.0),
        turnover = optDouble("turnover", 0.0),
        volumeRatio = optDouble("volumeRatio", 0.0),
        amplitude = optDouble("amplitude", 0.0),
        amount = optLong("amount"),
        industry = optString("industry").ifEmpty { "未分类" },
        benchmarkDelta = optNullableDouble("benchmarkDelta"),
        tags = optJSONArray("tags").toStringList(),
        aiProfile = AiProfile(
            action = profile?.optString("action") ?: AiLabels.ACTION_HOLD,
            signal = profile?.optString("signal") ?: AiLabels.SIGNAL_BOTTOM,
            score = profile?.optInt("score") ?: 0,
            scenario = profile?.optString("scenario") ?: AiLabels.SCENARIO_KEEP,
        ),
    )
}

private fun JSONObject?.toSummary(): AiSummary {
    if (this == null) return AiSummary("", 0L)
    return AiSummary(
        text = optString("text"),
        generatedAt = optLong("generatedAt"),
        stale = optBoolean("stale", false),
    )
}

private fun JSONObject.toAnalysis(): AiAnalysis? {
    if (optString("code").isEmpty()) return null
    val f = optJSONObject("factors")
    return AiAnalysis(
        trendLabel = optString("trendLabel"),
        trendText = optString("trendText"),
        riskLevel = optString("riskLevel"),
        riskText = optString("riskText"),
        score = optInt("score"),
        targetPrice = optLong("targetPrice"),
        stopLossPrice = optLong("stopLossPrice"),
        factors = AiFactors(
            momentum = f?.optInt("momentum") ?: 0,
            value = f?.optInt("value") ?: 0,
            risk = f?.optInt("risk") ?: 0,
            industry = f?.optString("industry") ?: "未分类",
            industryRank = f?.optInt("industryRank") ?: -1,
            benchmarkDelta = f?.optNullableDouble("benchmarkDelta"),
        ),
        source = optString("source").ifEmpty { "rules" },
    )
}

/** 可空数值：JSON 的 null 在 optDouble 下拿不到区别，用哨兵值再判空。 */
private fun JSONObject.optNullableDouble(name: String): Double? =
    optDouble(name, Double.NaN).takeIf { !it.isNaN() }

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optString(it) }.filter { it.isNotEmpty() }
}
