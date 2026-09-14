package com.example.aistock.data

/**
 * 大盘摘要（自选池口径）。
 *
 * ⚠️ 口径必须写清楚：`floatCapDelta` 是 **Σ 流通市值变动**（各股流通市值 × 当日涨跌幅 之和），
 * 不是「资金净流入」——两者常被混淆，但含义完全不同，前者只是市值变化的算术结果。
 */
data class MarketOverview(
    val up: Int,
    val down: Int,
    val flat: Int,
    val avgChangePct: Double,
    val floatCapDelta: Long,
    val generatedAt: Long,
)

fun deriveMarketOverview(stocks: List<StockItem>, generatedAt: Long): MarketOverview {
    if (stocks.isEmpty()) {
        return MarketOverview(0, 0, 0, 0.0, 0L, generatedAt)
    }
    val up = stocks.count { it.changePct > 0 }
    val down = stocks.count { it.changePct < 0 }
    val flat = stocks.size - up - down
    val avg = stocks.map { it.changePct }.average()
    val delta = stocks.sumOf { (it.floatCap * it.changePct / 100.0).toLong() }
    return MarketOverview(up, down, flat, avg, delta, generatedAt)
}
