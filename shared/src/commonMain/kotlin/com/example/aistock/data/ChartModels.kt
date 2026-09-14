package com.example.aistock.data

import kotlin.math.abs

/**
 * 走势序列（客户端模型）——分时。
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
 * 一条完整的分时序列。
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

/**
 * 一根 K 线（蜡烛）。中国市场惯例：**收盘 ≥ 开盘为阳线（红）**，
 * 判断就一行 `close >= open`，所以契约里不带冗余的「阴阳」字段。
 */
data class Candle(
    val time: String,      // 展示用标签：day="2026-09-07"、m60="09-11 15:00"
    val open: Long,        // 开盘（分）
    val close: Long,       // 收盘（分）
    val high: Long,        // 最高（分）
    val low: Long,         // 最低（分）
    val volume: Long,      // 成交量（手）
) {
    /** 阳线（收 ≥ 开）：红。 */
    val bullish: Boolean get() = close >= open
}

/**
 * 一条完整的 K 线序列（m60 / day / week / month）。
 */
data class KlineSeries(
    val token: String,
    val code: String,
    val name: String,
    val period: String,        // m60 / day / week / month
    val prevClose: Long,       // 昨收（分），首日涨跌色基准
    val candles: List<Candle>,
) {
    /** 可见域内（默认全部）的最高/最低，缩放时由图表按窗口重算。 */
    val high: Long get() = candles.maxOfOrNull { it.high } ?: prevClose
    val low: Long get() = candles.minOfOrNull { it.low } ?: prevClose

    /** 最新收盘相对昨收的涨跌幅（%）。 */
    val changePct: Double
        get() {
            val last = candles.lastOrNull() ?: return 0.0
            if (prevClose <= 0L) return 0.0
            return (last.close - prevClose).toDouble() / prevClose.toDouble() * 100.0
        }
}

/**
 * 图表数据的统一出口：分时（点序列）与 K 线（区间序列）是两种几何形状，
 * 密封类让 `when` 分支**漏写就编译不过**——新增周期类型时编译器会逼着
 * 所有图表代码同步适配，而不是运行时画出一半的图。
 */
sealed interface ChartData {
    /** 分时：一条价格线 + 均价线 + 成交量。 */
    data class Minute(val series: ChartSeries) : ChartData

    /** 蜡烛图：OHLC 四价 + 成交量。 */
    data class Kline(val series: KlineSeries) : ChartData
}

/** 支持的周期常量（与后端契约一一镜像，改这里必须同时改后端 Routes）。 */
object ChartPeriods {
    const val MINUTE = "minute"
    const val M60 = "m60"
    const val DAY = "day"
    const val WEEK = "week"
    const val MONTH = "month"

    /** 切换条上的顺序（分时在最左，与主流行情 App 一致）。 */
    val ALL = listOf(MINUTE, M60, DAY, WEEK, MONTH)

    /** 展示名。 */
    fun label(period: String): String = when (period) {
        MINUTE -> "分时"
        M60 -> "60分"
        DAY -> "日K"
        WEEK -> "周K"
        MONTH -> "月K"
        else -> period
    }

    /** 初始可见的蜡烛根数（时间跨度默认档）：日 K 一屏约 3 个月。 */
    fun defaultSpan(period: String): Int = when (period) {
        M60 -> 48          // 两天
        DAY -> 60          // 一季度
        WEEK -> 52         // 一年
        MONTH -> 36        // 三年
        else -> 240        // 分时用不到缩放
    }

    /** 缩放上下限：最少 10 根（看得清单根），最多全量。 */
    fun spanBounds(period: String, total: Int): IntRange {
        val max = if (total < 10) total.coerceAtLeast(1) else total
        val min = 10.coerceAtMost(max)
        return min..max
    }
}

/** 一个图表窗口：可见起始下标 + 可见根数。缩放/平移就是改这两个数。 */
data class ChartWindow(val start: Int, val count: Int) {
    init {
        require(start >= 0 && count > 0) { "invalid window: start=$start count=$count" }
    }

    val endExclusive: Int get() = start + count

    /** 平移 [delta] 根（正=向右看历史），夹在数据范围内。 */
    fun pan(delta: Int, total: Int): ChartWindow {
        val newStart = (start + delta).coerceIn(0, (total - count).coerceAtLeast(0))
        return copy(start = newStart)
    }

    /**
     * 以 [anchorFrac]（0~1，缩放中心在窗口内的相对位置）为中心缩放 [factor] 倍。
     * 上下限由调用方按周期算好传入（[ChartPeriods.spanBounds]）——
     * 窗口是纯几何对象，不该知道「自己是哪个周期」。
     */
    fun zoom(factor: Float, anchorFrac: Float, total: Int, minSpan: Int, maxSpan: Int): ChartWindow {
        if (abs(factor - 1f) < 0.01f) return this
        val anchor = start + count * anchorFrac
        val newCount = (count / factor).toInt().coerceIn(minSpan.coerceAtMost(total), maxSpan.coerceAtMost(total).coerceAtLeast(minSpan.coerceAtMost(total)))
        var newStart = (anchor - newCount * anchorFrac).toInt()
        newStart = newStart.coerceIn(0, (total - newCount).coerceAtLeast(0))
        return ChartWindow(newStart, newCount)
    }

    companion object {
        /** 全量窗口。 */
        fun full(total: Int, span: Int): ChartWindow {
            if (total <= 0) return ChartWindow(0, 1)
            val count = span.coerceAtMost(total)
            return ChartWindow((total - count).coerceAtLeast(0), count)
        }
    }
}
