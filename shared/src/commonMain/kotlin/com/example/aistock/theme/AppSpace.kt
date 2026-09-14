package com.example.aistock.theme

import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 间距 token —— 设计系统的「尺子」。
 *
 * 为什么单独抽一层：间距是最容易随手写数字的地方（这里 7.dp、那里 13.dp），
 * 结果整个 App 的节奏是乱的。收敛成有限的几档之后，UI 自然会「对齐」，
 * 而且将来要整体调松/调紧，只改这一个文件。
 *
 * 档位是 4dp 的倍数（业界通用的 4pt grid）。
 */
object AppSpace {
    /** 4dp：紧贴元素之间，如标签内边距。 */
    val Xs = 4.dp

    /** 8dp：同一组内的元素间距。 */
    val Sm = 8.dp

    /** 12dp：卡片内部各块之间。 */
    val Md = 12.dp

    /** 16dp：卡片与卡片之间。 */
    val Lg = 16.dp

    /** 20dp：大区块之间。 */
    val Xl = 20.dp

    /** 页面左右留白。所有页面统一用它，横向视觉才是齐的。 */
    val ScreenEdge = 12.dp
}
