package com.example.aistock.components.core

import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppShape
import com.example.aistock.theme.AppSpace
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 周期/维度切换条：分时 / 60分 / 日K / 周K / 月K，以及自选页的 全部 / 沪深 / 港股。
 *
 * 选中态用「墨块反白」——像铅字排版里选中的铅字块，而不是常见的药丸高亮：
 * 这是纸墨风里少数允许的实心填充，因为这个控件承担的是「排版工具」的角色，
 * 和数据卡片是两类东西。
 *
 * 布局细节：文字放在**固定高度的 Box 里水平垂直居中**——之前直接给 Text 加
 * vertical padding，中文字形行高导致墨块里的字明显偏上（实测踩坑）。
 */
@Composable
fun PeriodSwitcher(
    periods: List<String>,
    selected: String,
    label: (String) -> String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        periods.forEachIndexed { i, p ->
            val isSelected = p == selected
            Box(
                modifier = Modifier
                    .clickable { onSelect(p) }
                    .background(
                        color = if (isSelected) AppColors.Accent else Color.Transparent,
                        shape = AppShape.Badge,
                    )
                    .width(52.dp)
                    .height(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(p),
                    color = if (isSelected) AppColors.Panel else AppColors.TextSub,
                    fontSize = AppText.Small,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
            if (i < periods.size - 1) {
                Spacer(modifier = Modifier.width(AppSpace.Sm))
            }
        }
    }
}
