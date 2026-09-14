package com.example.aistock.backend.parse

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToLong

/** K 线序列（解析结果，未加工成对外 DTO）。价格单位：分。 */
internal data class KlineSeriesRaw(
    val name: String,
    val prevClose: Long,
    val candles: List<KlineCandleRaw>,
)

internal data class KlineCandleRaw(
    val time: String,
    val open: Long,
    val close: Long,
    val high: Long,
    val low: Long,
    val volume: Long,
)

/**
 * 腾讯 K 线解析，覆盖两类上游：
 *
 *  1. **fqkline**（日/周/月，前复权）：`data.{token}.qfqday` 等 key 下是
 *     `["2026-09-07","1324.000","1316.010","1333.600","1312.660","25250.000"]`
 *     —— `[日期, 开, 收, 高, 低, 量]`，全字符串，价格单位**元**；
 *  2. **mkline**（60 分钟）：`data.{token}.m60` 是
 *     `["202609111500","1278.93","1275.16","1279.50","1275.01","7294.28",{},"5.8350"]`
 *     —— 中间混着 `{}` 占位对象和多余字段，**必须按下标取、容错跳过非数字**。
 *
 * 阴阳判断留给客户端（close vs open），这里只做归一化：价格统一「分」。
 */
internal object KlineParser {

    fun parse(raw: String, token: String, period: String): KlineSeriesRaw? {
        val root = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        // 全程安全 cast，绝不用 jsonObject()：上游结构变体多（fqkline 的 data.sh600519
        // 理论上应为对象，但一旦上游改结构，防御式写法只会优雅地返回 null 走 404，
        // 而不是抛异常变 500 —— 404 和 500 的排障方向完全不同）
        val dataEl = root["data"] as? kotlinx.serialization.json.JsonObject ?: return null
        val node = dataEl[token] as? kotlinx.serialization.json.JsonObject ?: return null

        // 数据数组的 key：前复权 K 是 qfqday/qfqweek/qfqmonth，分钟线是 m60
        val dataKey = when (period) {
            "m60" -> "m60"
            else -> "qfq$period"
        }
        val arr = node[dataKey] as? JsonArray ?: (node[period] as? JsonArray) ?: return null

        val candles = arr.mapNotNull { el ->
            val row = el as? JsonArray ?: return@mapNotNull null
            val s = { i: Int -> (row.getOrNull(i) as? JsonPrimitive)?.content }
            // 布局：[时间, 开, 收, 高, 低, 量, ...]；mkline 第 7 位是 {} 占位
            val time = s(0) ?: return@mapNotNull null
            val open = s(1)?.toDoubleOrNull() ?: return@mapNotNull null
            val close = s(2)?.toDoubleOrNull() ?: return@mapNotNull null
            val high = s(3)?.toDoubleOrNull() ?: return@mapNotNull null
            val low = s(4)?.toDoubleOrNull() ?: return@mapNotNull null
            val volume = s(5)?.toDoubleOrNull() ?: 0.0
            if (open <= 0.0 || close <= 0.0 || high <= 0.0 || low <= 0.0) return@mapNotNull null
            KlineCandleRaw(
                time = normalizeTime(time, period),
                open = open.toCents(),
                close = close.toCents(),
                high = high.toCents(),
                low = low.toCents(),
                volume = volume.roundToLong(),
            )
        }
        if (candles.isEmpty()) return null

        // prec = 昨收（上游同时给出）；兜底用第一根的开盘
        val prevClose = (node["prec"] as? JsonPrimitive)?.content?.toDoubleOrNull()?.toCents()
            ?: candles.first().open

        // 名称在 qt 段（位号 1，与行情快照同一约定）；没有就退化为代码
        val name = ((node["qt"] as? kotlinx.serialization.json.JsonObject)
            ?.get(token) as? JsonArray)
            ?.getOrNull(1)?.let { (it as? JsonPrimitive)?.content } ?: token

        return KlineSeriesRaw(name = name, prevClose = prevClose, candles = candles)
    }

    /** 时间标签归一化成「一眼能读」的形式，时区无关的纯字符串处理。 */
    private fun normalizeTime(raw: String, period: String): String = when {
        // mkline: 202609111500 -> 09-11 15:00
        raw.length == 12 && raw.all { it.isDigit() } ->
            "${raw.substring(4, 6)}-${raw.substring(6, 8)} ${raw.substring(8, 10)}:${raw.substring(10, 12)}"
        // fqkline 已经是 2026-09-07；周/月线同理，原样返回
        else -> raw
    }

    private fun Double.toCents(): Long = (this * 100.0).roundToLong()
}
