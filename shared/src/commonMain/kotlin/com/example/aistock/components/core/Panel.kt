package com.example.aistock.components.core

import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppShape
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ColumnScope
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.shadow
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import androidx.compose.runtime.Composable

/**
 * 仪表面板：全 App 唯一的容器原语。
 *
 * 为什么不直接用「卡片 + 阴影」那套浅色设计的写法：**深色界面上阴影几乎看不见**，
 * 层次只能靠「底色差 + 1px 描边」表达。所以这里的浮起感是：
 *   页面底(#05070D) → 面板底(#0C1220) → 1px 描边(#1E2A40)
 * 选中/强调时把描边换成青色（[AppColors.Accent]），而不是换背景色——
 * 换背景会把里面红绿数字的对比度一起动掉。
 *
 * @param highlighted 强调态：青色描边（用于 AI 命中的条目、选中项）
 * @param glow 外发光。只在「需要抓注意力」的元素上开（如 AI 弹层），开多了就没重点了
 * @param onClick 传了才可点。点击态用统一的 [AppMotion] 微交互时长手感
 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    shape: Shape = AppShape.Card,
    highlighted: Boolean = false,
    glow: Boolean = false,
    paddingH: Dp = 14.dp,
    paddingV: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 强调态描边用半透明青：纯青描边在深底上会「越界」，压一点才像发光而不是画框
    val stroke = if (highlighted) AppColors.Accent.copy(alpha = 0.6f) else AppColors.Line

    Column(
        modifier = modifier
            .then(if (glow) Modifier.shadow(elevation = 10.dp, shape = shape, clip = false) else Modifier)
            .background(color = AppColors.Panel, shape = shape)
            .border(width = 1.dp, color = stroke, shape = shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = paddingH, vertical = paddingV),
        content = content,
    )
}
