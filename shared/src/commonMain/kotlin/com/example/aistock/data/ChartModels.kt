package com.example.aistock.data

/**
 * 走势序列（客户端模型）。
 *
 * 与行情快照同样的单位约定：价格/均价用**分**（Long），成交量用**手**。
 *
 * 为什么 `avg`（当日累计均价）由服务端给：它是「累计成交额 ÷ 累计成交量」，
 * 客户端要自己算就得先攒够全量点再逐点累加——不仅重复计算，还会因为
 * 客户端与后端取数时刻不同而算出两条不一样的均价线。放服务端算，只算一次。
 */
data class ChartPoint(
    val time: String,     // 0930
    val price: Long,      // 该分钟价格（分）
    val avg: Long,        // 当日累计均价（分）
    val volume: Long,     // 该分钟成交量（手）
)

/**
 * 一条完整的走势序列。
 *
 * [prevClose]（昨收）必须随数据一起传：分时图的涨跌基准是昨收而不是今开，
 * 没有它连"这条线是在涨还是在跌"都判断不了，横向基准线也画不出来。
 */
data class ChartSeries(
    val token: String,
    val code: String,
    val name: String,
    val period: String,        // minute
    val date: String,          // 20260914
    val prevClose: Long,       // 昨收（分）
    val points: List<ChartPoint>,
) {
    /** 最高价（含均价线，取两者更大者），用于算 Y 轴范围。 */
    val high: Long
        get() = points.maxOfOrNull { maxOf(it.price, it.avg) } ?: prevClose

    /** 最低价（含均价线）。 */
    val low: Long
        get() = points.minOfOrNull { minOf(it.price, it.avg) } ?: prevClose

    /** 相对昨收的涨跌幅（%）。今日无数据时返回 0。 */
    val changePct: Double
        get() {
            val last = points.lastOrNull() ?: return 0.0
            if (prevClose <= 0L) return 0.0
            return (last.price - prevClose).toDouble() / prevClose.toDouble() * 100.0
        }
}
