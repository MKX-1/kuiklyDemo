package com.example.aistock.backend.model

/**
 * 腾讯原始行情解析后的中间结构（不对外暴露）。
 *
 * 单位约定：价格类字段是**分**（Long），市值是**元**（Long）。
 * 用整数存钱、在解析层就完成单位换算，是金融数据处理的基本功。
 */
data class RawStock(
    val code: String,
    val market: String,
    val name: String,
    val price: Long,
    val change: Long,
    val changePct: Double,
    val high: Long,
    val low: Long,
    val open: Long,
    val marketCap: Long,
    val floatCap: Long,
    val pe: Double,
    val turnover: Double,
    val volumeRatio: Double,
    val amplitude: Double,
    val amount: Long,
)
