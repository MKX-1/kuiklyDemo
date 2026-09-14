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
 * 走势响应。
 *
 * `prevClose` 单独给出来而不是让客户端从第一个点推：分时的涨跌基准是**昨收**，
 * 不是今开。缺了它，涨跌颜色和横向基准线都会算错。
 */
@Serializable
data class ChartDto(
    val token: String,
    val code: String,
    val name: String,
    val period: String,        // minute（分时）；后续可扩 day/week/month
    val date: String,          // 20260914
    val prevClose: Long,       // 昨收（分）
    val points: List<ChartPointDto>,
)
