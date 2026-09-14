package com.example.aistock.data

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 当前时间（epoch 毫秒）。
 *
 * KMP 的 common 代码里没有 java.lang.System，跨端取时间要统一走一个封装，
 * 这样将来换实现（比如接入服务端时间）只改这一处。
 */
@OptIn(ExperimentalTime::class)
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * 分 → 元，保留两位小数。
 *
 * 不依赖 String.format：那是 JVM 专有 API，在 iOS/JS 端不存在，
 * 写在 commonMain 里会让跨端编译直接失败。所以用整数运算手写。
 */
fun formatFen(fen: Long): String = formatScaled(fen, 100, 2)

/** 两位小数的通用格式化（内部先放大再取整，避免浮点误差）。 */
fun formatDouble2(v: Double): String {
    val scaled = (v * 100).roundToLong()
    val neg = scaled < 0
    val abs = if (neg) -scaled else scaled
    val intPart = abs / 100
    val frac = abs % 100
    val fracStr = if (frac < 10) "0$frac" else "$frac"
    return (if (neg) "-" else "") + "$intPart.$fracStr"
}

/** 带符号百分比，如 "+2.35%" / "-1.20%"（0 显示为 "0.00%"）。 */
fun formatPctSigned(v: Double): String {
    val s = formatDouble2(v)
    return if (v > 0) "+$s%" else "$s%"
}

/** 不带符号的百分比，如 "2.35%"。 */
fun formatPct(v: Double): String = "${formatDouble2(v)}%"

/** 市值：元 → 「X.XX亿」/「X.XX万亿」。 */
fun formatCap(yuan: Long): String {
    val yi = yuan / 100_000_000.0
    return if (yi >= 10_000) "${formatDouble2(yi / 10_000)}万亿" else "${formatDouble2(yi)}亿"
}

/** 成交额：元 → 「X.XX亿」/「X.XX万」。 */
fun formatAmount(yuan: Long): String = when {
    yuan >= 100_000_000L -> "${formatDouble2(yuan / 100_000_000.0)}亿"
    yuan >= 10_000L -> "${formatDouble2(yuan / 10_000.0)}万"
    else -> "$yuan"
}

private fun formatScaled(value: Long, scale: Long, digits: Int): String {
    val neg = value < 0
    val abs = if (neg) -value else value
    val intPart = abs / scale
    val frac = abs % scale
    val padded = frac.toString().padStart(digits, '0')
    return (if (neg) "-" else "") + "$intPart.$padded"
}

/** 时间戳 → "HH:mm:ss"（东八区，纯算术，不引入日期库）。 */
fun formatHms(ms: Long): String {
    val bj = ms + 8 * 3600 * 1000L
    fun two(v: Long) = (v % 60).toString().padStart(2, '0')
    val h = ((bj / 3_600_000L) % 24).toString().padStart(2, '0')
    return "$h:${two(bj / 60_000L)}:${two(bj / 1000L)}"
}

/** 判断是否已过期（超过 5 分钟）。 */
fun isStale(generatedAt: Long, now: Long = nowMillis()): Boolean =
    now - generatedAt > FactorThresh.STALE_MS

/** 取绝对值（避免各页面重复 import）。 */
fun absDouble(v: Double): Double = abs(v)
