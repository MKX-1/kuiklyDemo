package com.example.aistock.data

import com.tencent.kuikly.core.module.NetworkModule

/**
 * 数据源路由：按「自建后端 → 直连腾讯 → 本地样例」逐级降级。
 *
 * 降级判据是「这一层有没有取到数据」，而不是捕获异常——网络失败在数据层已被吸收成空结果，
 * 所以这里没有 try/catch 森林。但 CancellationException 必须放行，否则协程取消会被吞掉。
 *
 * 命中哪一级由各级数据源自己盖章进 [WatchlistBundle.source]，页面据此显示实时/缓存/离线三态。
 */
class RoutedStockApi(
    private val backend: BackendApi?,
    private val live: StockApi,
    private val fallback: StockApi = SampleStockApi,
) : StockApi {

    override suspend fun fetchWatchlist(): WatchlistBundle {
        backend?.watchlist()?.let { return it }
        val liveBundle = live.fetchWatchlist()
        if (liveBundle.stocks.isNotEmpty()) return liveBundle
        return fallback.fetchWatchlist()
    }

    override suspend fun fetchStock(code: String): StockItem? {
        backend?.watchlist()?.stocks?.find { it.code == code }?.let { return it }
        live.fetchStock(code)?.let { return it }
        return fallback.fetchStock(code)
    }

    override suspend fun fetchAiAnalysis(code: String): AiAnalysis {
        // 后端不可用时，在真实行情上用本地规则引擎推导，保证「结论与行情同源」
        backend?.analysis(code)?.let { return it }
        live.fetchStock(code)?.let { return deriveAiAnalysis(it) }
        return fallback.fetchAiAnalysis(code)
    }
}

/**
 * 页面侧的唯一装配入口：一行拿到「带降级的行情源」。
 *
 * 需要 [NetworkModule] 才能构造（它依赖 Activity），所以由 Composable 注入——
 * 数据层本身不持有任何 UI 概念，这是分层的关键。
 */
object StockApis {

    /** 后端开关：只想看「直连腾讯 / 离线样例」时置 false。 */
    var backendEnabled: Boolean = true

    fun stocks(network: () -> NetworkModule): StockApi = RoutedStockApi(
        backend = if (backendEnabled) BackendApi(network) else null,
        live = TencentStockApi(network),
    )
}
