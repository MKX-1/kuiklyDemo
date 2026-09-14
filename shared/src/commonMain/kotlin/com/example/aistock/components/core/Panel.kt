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
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import androidx.compose.runtime.Composable

/**
 * 纸面板：全 App 唯一的容器原语。
 *
 * 报刊的层次表达（跟深色版相反，浅色纸上阴影反而可用，但**刻意不用**）：
 * 阴影是「悬浮物」的语言，纸是**贴在版面上的**——所以层次靠
 *   页面纸(#F5F0E6) → 面板纸(#FCF9F1) → 1px 淡墨描边(#D8CDB4)
 * 这也是文艺风和「互联网卡片风」的分界线：后者靠圆角+阴影，前者靠描边+留白。
 *
 * @param highlighted 强调态：黛青描边（AI 命中的条目、选中项）
 * @param glow 兼容参数：纸墨风不需要发光，保留签名以免改动调用方，忽略之
 * @param onClick 传了才可点
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
    val stroke = if (highlighted) AppColors.Accent else AppColors.Line

    Column(
        modifier = modifier
            .background(color = AppColors.Panel, shape = shape)
            .border(width = 1.dp, color = stroke, shape = shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = paddingH, vertical = paddingV),
        content = content,
    )
}
