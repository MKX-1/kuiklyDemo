package com.example.aistock.backend.parse

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToLong

/** 分时序列（解析结果，未加工成对外 DTO）。价格单位：分。 */
internal data class MinuteSeries(
    val date: String,          // 20260914
    val name: String,
    val prevClose: Long,       // 昨收（分）
    val points: List<MinutePoint>,
)

/** 分时上的一个点。 */
internal data class MinutePoint(
    val time: String,          // 0930
    val price: Long,           // 该分钟价格（分）
    val volume: Long,          // 该分钟成交量（手）
)

/**
 * 腾讯分时（`web.ifzq.gtimg.cn/appstock/app/minute/query`）解析。
 *
 * 这个接口别扭的地方：每根分时是一条**字符串** `"0930 1277.27 133 16987691.06"`
 * （`HHMM 价格 量(手) 额(元)`），而 `data.data` 这个字段在不同响应里可能是
 * JSON 数组、也可能是一整段字符串字面量 —— 所以两种形态都要接住，再统一用正则
 * 把四元组捞出来。正则的好处：上游哪天多带几个字段或改了引号风格，
 * 只要四元组的形状不变就还能 work，按逗号 split 的写法会立刻碎掉。
 *
 * 昨收在 `qt` 字段里，位号 4 —— 写死位号是不体面，但这段格式上游十几年没变，
 * 且一旦变了 [parse] 会返回 null，页面只会显示"走势不可用"，不会显示错误数据。
 */
internal object MinuteParser {

    /** `HHMM 价格 成交量(手) 成交额(元)`。 */
    private val POINT_PATTERN = Regex("""(\d{4})\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)""")

    /** 取出一段字符串字面量里被单引号包起来的字段（老形态：整段是字符串）。 */
    private val QUOTED = Regex("""'([^']*)'""")

    fun parse(raw: String, token: String): MinuteSeries? {
        val root = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        val node = root["data"]?.jsonObject?.get(token)?.jsonObject ?: return null

        val inner = node["data"]?.jsonObject
        val listLiteral = when (val d = inner?.get("data")) {
            is JsonArray -> d.mapNotNull { el -> runCatching { el.jsonPrimitive.content }.getOrNull() }
            is JsonPrimitive -> listOf(d.content)
            else -> return null
        }
        val date = inner["date"]?.jsonPrimitive?.content.orEmpty()

        val points = listLiteral
            // 每条目单独过正则；整段字面量形态则一条长文本里能捞出全部四元组
            .flatMap { POINT_PATTERN.findAll(it).mapNotNull { m ->
                val price = m.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
                val volume = m.groupValues[3].toDoubleOrNull() ?: return@mapNotNull null
                if (price <= 0.0) return@mapNotNull null
                MinutePoint(
                    time = m.groupValues[1],
                    price = price.toCents(),
                    volume = volume.roundToLong(),
                )
            } }

        if (points.isEmpty()) return null

        // qt 里那段：['1','贵州茅台','600519','1277.96','1275.16', ...]
        // 位号 1=名称、4=昨收（与 QuoteParser 同一套位号约定）。数组/字符串两种形态都接。
        val qtLiteral: List<String> = when (val q = node["qt"]?.jsonObject?.get(token)) {
            is JsonArray -> q.mapNotNull { el -> runCatching { el.jsonPrimitive.content }.getOrNull() }
            is JsonPrimitive -> QUOTED.findAll(q.content).map { it.groupValues[1] }.toList()
            else -> emptyList()
        }
        val name = qtLiteral.getOrNull(1).orEmpty()
        val prevClose = qtLiteral.getOrNull(4)?.toDoubleOrNull()?.toCents() ?: 0L

        return MinuteSeries(date = date, name = name, prevClose = prevClose, points = points)
    }

    private fun Double.toCents(): Long = (this * 100.0).roundToLong()
}
