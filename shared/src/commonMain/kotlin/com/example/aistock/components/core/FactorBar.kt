package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppShape
import com.example.aistock.theme.AppSpace
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 因子条：用一个水平进度条表达「这一项拿了多少分」。
 *
 * 为什么不用环形/雷达图：AI 评分是多因子（动量/价值/风险）并列比较，
 * 长度是最容易横向比较的视觉通道；环形图三四个摆在一起反而难比。
 *
 * 实现上用**两层 Box + fillMaxWidth(fraction)**，不碰 Canvas：
 * 宽度比例是布局问题，交给布局系统算比手绘更准，也不用处理尺寸变化。
 *
 * 轨道宽度的下限做了保护：`coerceIn(0f, 1f)` 之外还要求至少 2%，
 * 否则 0 分时轨道和填充重叠，看起来像「有个小尾巴」。
 */
@Composable
fun FactorBar(
    label: String,
    value: Int,
    max: Int,
    modifier: Modifier = Modifier,
    color: Color = AppColors.Accent,
) {
    val fraction = if (max <= 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    val fill = if (value <= 0) 0f else fraction.coerceAtLeast(0.02f)

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = AppColors.TextSub,
                fontSize = AppText.Caption,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$value",
                color = color,
                fontSize = AppText.Caption,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = " / $max",
                color = AppColors.TextWeak,
                fontSize = AppText.Micro,
            )
        }
        Spacer(modifier = Modifier.height(AppSpace.Xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(color = AppColors.PanelHi, shape = AppShape.Pill),
        ) {
            if (fill > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fill)
                        .height(5.dp)
                        .background(color = color, shape = AppShape.Pill),
                )
            }
        }
    }
}
