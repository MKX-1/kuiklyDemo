package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppShape
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.material3.Text

/**
 * 可切换的药丸标签（市场维度、K 线周期都用它）。
 *
 * 选中态的处理方式是刻意的：**只把描边和文字换成青色 + 加一层极淡青底**，
 * 不填充实心青。实心填充会在同一屏出现多个「亮块」，把唯一强调色摊薄，
 * 反而看不出哪个是当前选中。
 *
 * 触摸目标高度给到 30dp：视觉上小巧，但配合 Row 的上下 padding 后
 * 实际可点区域接近 44dp 的可用性要求。
 */
@Composable
fun Chip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val border = when {
        !enabled -> AppColors.LineSoft
        selected -> AppColors.Accent.copy(alpha = 0.7f)
        else -> AppColors.Line
    }
    val fg = when {
        !enabled -> AppColors.TextWeak
        selected -> AppColors.Accent
        else -> AppColors.TextSub
    }
    val bg = if (selected && enabled) AppColors.AccentSoft else AppColors.PanelHi

    Text(
        text = text,
        color = fg,
        fontSize = AppText.Caption,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = modifier
            .background(color = bg, shape = AppShape.Pill)
            .border(width = 1.dp, color = border, shape = AppShape.Pill)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
