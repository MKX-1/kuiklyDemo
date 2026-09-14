package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppColors
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 氛围图层：极淡的网格 + 一条横向扫描线。
 *
 * 这是整套「深空终端」视觉里**最便宜也最有效**的一笔：
 * 近黑底上什么都没有会显得空洞（像没加载完），加一层 4% 透明度的网格，
 * 立刻有了「仪表盘平面」的空间感，而且完全不抢内容——文字和数字仍然是最亮的。
 *
 * 注意：Canvas 靠 `size` 取尺寸，所以**外层必须给定高度**（固定高度或 fillMaxSize 的容器），
 * 否则高度为 0 什么也画不出来。它设计成放在内容底下的第一层使用。
 *
 * @param cell 网格边长。20–24dp 比较合适：太密显得脏，太疏看不出是网格
 * @param scanRatio 扫描线的纵向位置（0–1）。放在内容下三分之一处最像「扫描头刚扫过」
 */
@Composable
fun GridBackdrop(
    modifier: Modifier = Modifier,
    cell: Dp = 22.dp,
    scanRatio: Float = 0.82f,
) {
    Canvas(modifier = modifier) {
        val step = cell.toPx()
        val w = size.width
        val h = size.height
        if (step <= 0f || w <= 0f || h <= 0f) return@Canvas

        // 竖线
        var x = 0f
        while (x <= w) {
            drawLine(AppColors.GridLine, Offset(x, 0f), Offset(x, h), 1f)
            x += step
        }
        // 横线
        var y = 0f
        while (y <= h) {
            drawLine(AppColors.GridLine, Offset(0f, y), Offset(w, y), 1f)
            y += step
        }
        // 扫描线：一条比网格亮一档的青线，暗示「这是一个在持续刷新的面板」
        val sy = h * scanRatio
        drawLine(AppColors.Accent.copy(alpha = 0.10f), Offset(0f, sy), Offset(w, sy), 1.5f)
    }
}
