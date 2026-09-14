package com.example.aistock.backend.parse

import com.example.aistock.backend.model.RawStock
import kotlin.math.roundToLong

/**
 * 腾讯行情「~ 分隔协议」解析（纯函数）。
 *
 * 这是整个后端存在的**最主要理由**：上游给的是 `~` 分隔的 GBK 文本，
 * 字段散落在几十个下标里，还混着单位不一致（A 股成交额是"万"、港股是"元"）。
 * 把这些脏活全收在这里，客户端就只需要拿干净的 JSON。
 *
 * 位号已对照真实响应核实，请勿凭直觉改：
 *  - 21..24 是买卖五档，不是涨跌数据；
 *  - 32..35 看着像涨跌幅/最高/最低，实际会错位；
 *  - PE 在 A 股与港股都是 39，40 位恒为空；
 *  - 量比：A 股在 49，港股该位是 52 周区间（所以港股量比恒 0，不能假算）。
 */
object QuoteParser {

    private const val NAME = 1
    private const val CODE = 2
    private const val PRICE = 3
    private const val OPEN = 5
    private const val CHANGE = 31
    private const val CHANGE_PCT = 32
    private const val HIGH = 33
    private const val LOW = 34
    private const val PE = 39
    private const val AMPLITUDE = 43
    private const val FLOAT_CAP = 44       // 亿
    private const val MARKET_CAP = 45      // 亿
    private const val AMOUNT = 37          // A 股单位「万」，港股是「元」
    private const val TURNOVER_A = 38
    private const val TURNOVER_HK = 59
    private const val VOLUME_RATIO_A = 49

    /** 从整体响应文本里抠出 `v_<token>="…"` 引号内的内容。 */
    fun extractBody(raw: String?, token: String): String? {
        if (raw == null) return null
        val key = "v_$token="
        val at = raw.indexOf(key)
        if (at < 0) return null
        val start = raw.indexOf('"', at)
        if (start < 0) return null
        val end = raw.indexOf('"', start + 1)
        if (end < 0) return null
        return raw.substring(start + 1, end)
    }

    /** 只取涨跌幅（给大盘指数用）。 */
    fun changePctOnly(body: String?): Double? {
        if (body == null) return null
        val f = body.split("~")
        if (f.size <= CHANGE_PCT) return null
        return f[CHANGE_PCT].toDoubleOrNull()
    }

    /** 解析单只股票；关键字段不足返回 null（调用方计为 missing）。 */
    fun parse(body: String?, market: String): RawStock? {
        if (body == null) return null
        val f = body.split("~")
        if (f.size <= LOW) return null
        fun at(i: Int): String = if (i < f.size) f[i] else ""
        return RawStock(
            code = at(CODE),
            market = market,
            name = at(NAME),
            price = yuanToFen(at(PRICE)),
            change = yuanToFen(at(CHANGE)),
            changePct = at(CHANGE_PCT).toDoubleOrNull() ?: 0.0,
            high = yuanToFen(at(HIGH)),
            low = yuanToFen(at(LOW)),
            open = yuanToFen(at(OPEN)),
            marketCap = yiToYuan(at(MARKET_CAP)),
            floatCap = yiToYuan(at(FLOAT_CAP)),
            pe = at(PE).toDoubleOrNull() ?: 0.0,
            turnover = at(if (market == "hk") TURNOVER_HK else TURNOVER_A).toDoubleOrNull() ?: 0.0,
            volumeRatio = if (market == "hk") 0.0 else (at(VOLUME_RATIO_A).toDoubleOrNull() ?: 0.0),
            amplitude = at(AMPLITUDE).toDoubleOrNull() ?: 0.0,
            amount = if (market == "hk") {
                at(AMOUNT).toDoubleOrNull()?.roundToLong() ?: 0L
            } else {
                // A 股成交额原始单位是「万」，统一归一成「元」
                at(AMOUNT).toDoubleOrNull()?.let { (it * 10_000.0).roundToLong() } ?: 0L
            },
        )
    }

    private fun yuanToFen(s: String): Long = s.toDoubleOrNull()?.let { (it * 100.0).roundToLong() } ?: 0L

    private fun yiToYuan(s: String): Long = s.toDoubleOrNull()?.let { (it * 100_000_000.0).roundToLong() } ?: 0L
}
