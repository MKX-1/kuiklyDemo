package com.example.aistock.backend.dto

import kotlinx.serialization.Serializable

/**
 * 对外 JSON 契约（与客户端 `StockModels.kt` 的领域模型一一对应）。
 *
 * 契约由**客户端定义**：客户端声明它想要什么形状，后端负责把腾讯的脏数据转换成这个形状。
 * 改这里的字段就要同步改客户端解析，反之亦然——这是"契约"的含义。
 */

@Serializable
data class AiProfileDto(
    val action: String,
    val signal: String,
    val score: Int,
    val scenario: String,
)

@Serializable
data class AiSummaryDto(
    val text: String,
    val generatedAt: Long,
    val stale: Boolean = false,
)

@Serializable
data class AiFactorsDto(
    val momentum: Int,   // 0-60
    val value: Int,      // 0-25
    val risk: Int,       // 0-8
    val industry: String,
    val industryRank: Int = -1,
    val benchmarkDelta: Double? = null,
)

/** 单位：价格/涨跌/高低/开 = 分；市值/流通市值/成交额 = 元。 */
@Serializable
data class StockItemDto(
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
    val aiProfile: AiProfileDto,
    val turnover: Double = 0.0,
    val volumeRatio: Double = 0.0,
    val amplitude: Double = 0.0,
    val amount: Long = 0L,
    val industry: String = "未分类",
    val benchmarkDelta: Double? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class WatchlistResponseDto(
    val stocks: List<StockItemDto>,
    val missing: Int,
    val summary: AiSummaryDto? = null,
)

@Serializable
data class AiAnalysisDto(
    val code: String,
    val name: String,
    val trendLabel: String,
    val trendText: String,
    val riskLevel: String,
    val riskText: String,
    val score: Int,
    val targetPrice: Long,
    val stopLossPrice: Long,
    val factors: AiFactorsDto,
)

@Serializable
data class ErrorDto(val error: String)

@Serializable
data class HealthDto(val status: String)
