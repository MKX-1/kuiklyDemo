package com.example.aistock.backend.dto

import kotlinx.serialization.Serializable

/**
 * 分时走势的一个点。
 *
 * 单位与全项目一致：价格**分**（Long），成交量**手**。
 * `avg` 是当日累计均价（累计成交额 ÷ 累计成交量），由服务端算好下发 ——
 * 客户端要做这件事得先攒够全量数据再逐点累加，纯属把计算放在错误的地方。
 */
@Serializable
data class ChartPointDto(
    val time: String,     // 0930
    val price: Long,      // 该分钟价格（分）
    val avg: Long,        // 当日累计均价（分）
    val volume: Long,     // 该分钟成交量（手）
)

/**
 * 分时走势响应。
 *
 * `prevClose` 单独给出来而不是让客户端从第一个点推：分时的涨跌基准是**昨收**，
 * 不是今开。缺了它，涨跌颜色和横向基准线都会算错。
 */
@Serializable
data class ChartDto(
    val token: String,
    val code: String,
    val name: String,
    val period: String,        // minute
    val date: String,          // 20260914
    val prevClose: Long,       // 昨收（分）
    val points: List<ChartPointDto>,
)

/**
 * K 线（蜡烛）的一个周期。
 *
 * 中国市场惯例：**收盘 ≥ 开盘为阳线（红），反之为阴线（绿）**——
 * 客户端直接拿 open/close 比较即可，服务端不做冗余的「阴阳」字段。
 */
@Serializable
data class CandleDto(
    val time: String,      // 展示用标签：day="2026-09-07"、m60="09-11 15:00"、week="2026-09-01"
    val open: Long,        // 开盘（分）
    val close: Long,       // 收盘（分）
    val high: Long,        // 最高（分）
    val low: Long,         // 最低（分）
    val volume: Long,      // 成交量（手）
)

/**
 * K 线响应（日 / 周 / 月 / 60 分钟）。
 *
 * 与分时 [ChartDto] 分成两个 DTO 而不是一个「万能图表」结构：
 * 分时是**点序列**（每分钟一个价），K 线是**区间序列**（每根含 OHLC 四价），
 * 硬塞进一个结构必然出现一堆「分时用不到 / K线用不到」的可空字段，
 * 契约就说不清了。
 */
@Serializable
data class KlineDto(
    val token: String,
    val code: String,
    val name: String,
    val period: String,        // m60 / day / week / month
    val prevClose: Long,       // 昨收（分），K 线首日涨跌色基准
    val candles: List<CandleDto>,
)
