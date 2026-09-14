package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppSpace
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 键值格：上「标签」下「数值」，详情页的指标区由它拼格。
 *
 * 用「标签小 + 数值大 + 标签弱色 + 数值强色」做对比，而不是给数值加边框：
 * 一屏十几个指标时，边框会变成一堆噪点。层次靠**字号和明度差**表达最干净。
 *
 * @param valueColor 数值颜色。涨跌类指标传语义色，其余保持默认的正文色
 * @param emphasis 是否加大数值（用于「现价」「市值」这类主指标）
 */
@Composable
fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AppColors.TextMain,
    emphasis: Boolean = false,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = AppColors.TextWeak,
            fontSize = AppText.Micro,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = if (emphasis) AppText.Title else AppText.Body,
            fontWeight = if (emphasis) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
