package com.example.aistock.data

/**
 * 离线样例数据源（兜底链的最后一级）。
 *
 * ⚠️ 这里的数值是**演示占位数据，不是真实行情**。
 * 存在的唯一意义：网络和后端全都不可用时，App 不至于白屏，用户还能看到界面长什么样。
 * 因此它返回的 [WatchlistBundle.source] 恒为 [DataSource.OFFLINE]，
 * 页面必须据此打出「展示示例数据」横幅——把示例数据伪装成实时行情是绝对不允许的。
 *
 * benchmarkDelta 一律为 null：没有真实指数数据就不填，UI 会显示「—」。
 */
object SampleStockApi : StockApi {

    /** 自选清单（与真实数据源保持同一批标的）。 */
    private val raw: List<StockItem> = listOf(
        sample("sh600519", "600519", "贵州茅台", 185600, 4262, 2.35, 185100, 187200, 184500, 32.0, "白酒", 1.1, 1.4, 4_170_000_000L),
        sample("hk00700", "00700", "腾讯控股", 39640, 475, 1.20, 39200, 39850, 39000, 18.6, "互联网", 0.8, 1.1, 6_800_000_000L),
        sample("sz300750", "300750", "宁德时代", 23520, -188, -0.80, 23750, 23900, 23300, 22.4, "电池", 1.5, 1.6, 2_600_000_000L),
        sample("sz002594", "002594", "比亚迪", 28600, 887, 3.10, 27750, 28800, 27500, 24.0, "汽车", 2.6, 2.1, 3_200_000_000L),
        sample("sh601318", "601318", "中国平安", 4820, -24, -0.50, 4850, 4900, 4780, 8.6, "保险", 0.6, 0.9, 1_500_000_000L),
        sample("sz000858", "000858", "五粮液", 13860, 90, 0.65, 13800, 14000, 13600, 19.8, "白酒", 0.9, 1.0, 1_800_000_000L),
        sample("sh688981", "688981", "中芯国际", 9870, 415, 4.20, 9480, 10000, 9400, 48.0, "半导体", 2.8, 2.3, 1_200_000_000L),
    )

    override suspend fun fetchWatchlist(): WatchlistBundle {
        val now = nowMillis()
        // 指数未知 → benchmarkDelta 传 null（enrich 会保留 null，不会编一个数字）
        val enriched = raw.map { enrich(it, null) }
        return WatchlistBundle(
            stocks = enriched,
            fetchedAt = now,
            source = DataSource.OFFLINE,
            summary = deriveSummary(enriched, now),
            missing = 0,
        )
    }

    override suspend fun fetchStock(code: String): StockItem? =
        raw.find { it.code == code }?.let { enrich(it, null) }

    /**
     * 走势：离线样例**刻意不给**。
     *
     * 样例数据本来就是演示占位，但"占位行情"和"占位走势曲线"性质不同：
     * 前者一眼能看出是假的（角标会写「示例数据」），后者画成曲线后**看起来就是真的**。
     * 宁可让详情页显示「暂无走势数据」，也不给一条编出来的线。
     */
    override suspend fun fetchChart(token: String, period: String): ChartData? = null

    override suspend fun fetchAiAnalysis(code: String): AiAnalysis {
        val item = fetchWatchlist().stocks.firstOrNull { it.code == code }
            ?: fetchWatchlist().stocks.first()
        return deriveAiAnalysis(item)
    }

    private fun sample(
        token: String, code: String, name: String,
        price: Long, change: Long, changePct: Double,
        open: Long, high: Long, low: Long,
        pe: Double, industry: String,
        turnover: Double, volumeRatio: Double, amount: Long,
    ): StockItem {
        val base = StockItem(
            id = token, code = code, name = name,
            price = price, change = change, changePct = changePct,
            open = open, high = high, low = low,
            marketCap = 0L, floatCap = 0L, pe = pe,
            turnover = turnover, volumeRatio = volumeRatio, amount = amount,
            industry = industry,
        )
        return base.copy(aiProfile = deriveAiProfile(base))
    }
}
