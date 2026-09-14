package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppColors
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 矢量图标集：全部用 Canvas 手绘，**不用 PNG 资源**。
 *
 * 三个理由：
 *  1. 图标颜色由参数决定，能跟着主题 / 涨跌色走 —— 位图做不到，浅色底和深色底要出两套图；
 *  2. 少一层 assets 依赖，将来出 H5 / 鸿蒙端不用做资源适配；
 *  3. 线条图标天然贴合「仪表盘」这套视觉语言（细线、圆头端点）。
 *
 * 绘制约定：坐标全部按 `size` 的比例算，这样同一个图标能在任意尺寸下保持形状。
 */
@Composable
fun ChevronBack(
    modifier: Modifier = Modifier,
    color: Color = AppColors.TextMain,
    strokeWidth: Dp = 1.8.dp,
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val half = stroke / 2f
        val elbow = Offset(size.width * 0.38f, size.height * 0.5f)
        drawLine(color, Offset(size.width * 0.66f, half), elbow, stroke, StrokeCap.Round)
        drawLine(color, elbow, Offset(size.width * 0.66f, size.height - half), stroke, StrokeCap.Round)
    }
}

/** 下向箭头。折叠区开关用它，配合外层的 `rotate()` 就能表达「收起/展开」。 */
@Composable
fun ChevronDown(
    modifier: Modifier = Modifier,
    color: Color = AppColors.TextSub,
    strokeWidth: Dp = 1.8.dp,
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val half = stroke / 2f
        val elbow = Offset(size.width * 0.5f, size.height * 0.66f)
        drawLine(color, Offset(half, size.height * 0.34f), elbow, stroke, StrokeCap.Round)
        drawLine(color, elbow, Offset(size.width - half, size.height * 0.34f), stroke, StrokeCap.Round)
    }
}

/** 对勾。用于「已加载」「命中规则」等肯定的状态。 */
@Composable
fun CheckMark(
    modifier: Modifier = Modifier,
    color: Color = AppColors.Accent,
    strokeWidth: Dp = 1.8.dp,
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        drawLine(
            color,
            Offset(size.width * 0.22f, size.height * 0.54f),
            Offset(size.width * 0.42f, size.height * 0.74f),
            stroke, StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(size.width * 0.42f, size.height * 0.74f),
            Offset(size.width * 0.78f, size.height * 0.28f),
            stroke, StrokeCap.Round,
        )
    }
}

/**
 * AI 星标（四角星）。用来标记「这条是 AI 给的」，比放一个「AI」文字块更轻。
 *
 * 四角星用四条线画十字再收窄，比 Bezier 曲线简单，且在 12dp 这种小尺寸下更清晰。
 */
@Composable
fun AiSpark(
    modifier: Modifier = Modifier,
    color: Color = AppColors.Accent,
    strokeWidth: Dp = 1.2.dp,
) {
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val r = size.width * 0.46f
        // 竖向长轴
        drawLine(color, Offset(cx, cy - r), Offset(cx, cy + r), stroke, StrokeCap.Round)
        // 横向短轴（0.62 让它是「星」而不是「十字」）
        drawLine(color, Offset(cx - r * 0.62f, cy), Offset(cx + r * 0.62f, cy), stroke, StrokeCap.Round)
        // 两条斜向短轴
        val d = r * 0.42f
        drawLine(color, Offset(cx - d, cy - d), Offset(cx + d, cy + d), stroke, StrokeCap.Round)
        drawLine(color, Offset(cx + d, cy - d), Offset(cx - d, cy + d), stroke, StrokeCap.Round)
    }
}
