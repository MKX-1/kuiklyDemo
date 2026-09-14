package com.example.aistock.data

/**
 * 自选列表的维度过滤 —— 纯函数，不持有状态。
 *
 * 维度（按市场分）是自选页最稳的一刀：按「行业/概念」分组需要行业库全量覆盖
 * （我们只有 7 只票的静态映射），按「涨跌」分组会随行情抖动跳变——都不如市场维度可靠。
 * 「持仓」维度等接入账户体系后再加，这里留枚举位。
 */
object WatchlistFilter {

    const val ALL = "all"
    const val CN = "cn"
    const val HK = "hk"

    /** 维度切换条的全部档位。 */
    val DIMENSIONS = listOf(ALL, CN, HK)

    fun label(dim: String): String = when (dim) {
        ALL -> "全部"
        CN -> "沪深"
        HK -> "港股"
        else -> dim
    }

    /** token 前缀 → 是否属于该维度。 */
    fun matches(dim: String, token: String?): Boolean = when (dim) {
        ALL -> true
        CN -> token?.startsWith("sh") == true || token?.startsWith("sz") == true
        HK -> token?.startsWith("hk") == true
        else -> true
    }

    /** 组头展示名：维度 + 组内数量。 */
    fun groupTitle(dim: String, count: Int): String =
        "${label(dim)} · $count 只"
}
