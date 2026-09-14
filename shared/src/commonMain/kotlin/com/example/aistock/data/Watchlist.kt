package com.example.aistock.data

/**
 * 自选清单的单一事实源：只维护「市场前缀 + 代码」。
 *
 * 名称不走本地硬编码，而是尽量用数据源返回的真名（后端会做 GBK 解码）。
 * 直连腾讯时如果中文被网络层解码坏了，才回落到这里的兜底名。
 */
object Watchlist {

    /** token 形如 sh600519 / sz300750 / hk00700（后端接口也吃这个格式）。 */
    val tokens: List<String> = listOf(
        "sh600519",   // 贵州茅台
        "hk00700",    // 腾讯控股
        "sz300750",   // 宁德时代
        "sz002594",   // 比亚迪
        "sh601318",   // 中国平安
        "sz000858",   // 五粮液
        "sh688981",   // 中芯国际
    )

    /** 直连模式下中文名的兜底（仅在网络层解码失败时使用）。 */
    private val fallbackNames: Map<String, String> = mapOf(
        "sh600519" to "贵州茅台",
        "hk00700" to "腾讯控股",
        "sz300750" to "宁德时代",
        "sz002594" to "比亚迪",
        "sh601318" to "中国平安",
        "sz000858" to "五粮液",
        "sh688981" to "中芯国际",
    )

    /**
     * 行业静态表（直连腾讯时用）。
     * ⚠️ 与后端 `IndustryMap` 保持一致的单一事实源思路：两边不同步就会出现
     * 「同一条自选，走后端显示白酒、走直连显示未分类」这种不一致。
     */
    private val industries: Map<String, String> = mapOf(
        "600519" to "白酒",
        "000858" to "白酒",
        "00700" to "互联网",
        "300750" to "电池",
        "002594" to "汽车",
        "601318" to "保险",
        "688981" to "半导体",
    )

    val query: String get() = tokens.joinToString(",")

    fun marketOf(token: String): String = token.substring(0, 2)

    fun codeOf(token: String): String = token.substring(2)

    fun fallbackName(token: String): String = fallbackNames[token] ?: codeOf(token)

    fun industryOf(code: String): String = industries[code] ?: "未分类"

    /** 纯代码 → token；不在自选清单里返回 null。 */
    fun tokenOf(code: String): String? = tokens.firstOrNull { codeOf(it) == code }
}
