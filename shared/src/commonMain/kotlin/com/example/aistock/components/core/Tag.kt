package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppShape
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 静态标签（「强于大盘」「放量上攻」这类规则命中的结果）。
 *
 * 与 [Chip] 的区别：Chip 是**可交互**的筛选项，Tag 是**只读**的数据标注。
 * 两者视觉故意做得像（同族元素），但 Tag 不带点击态。
 *
 * 颜色由调用方传入语义色（如涨跌色或强调色），面板只负责把它压成
 * 「淡底 + 半透明描边 + 同色文字」这一种表达 —— 深底上直接用纯色文字会糊成一团。
 */
@Composable
fun Tag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        fontSize = AppText.Micro,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .background(color = color.copy(alpha = 0.12f), shape = AppShape.Badge)
            .border(width = 1.dp, color = color.copy(alpha = 0.30f), shape = AppShape.Badge)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
