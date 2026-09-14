package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppShape
import com.example.aistock.theme.AppSpace
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 周期切换条：分时 / 60分 / 日K / 周K / 月K。
 *
 * 选中态用「墨块反白」——像铅字排版里选中的铅字块，而不是常见的药丸高亮：
 * 这是纸墨风里少数允许的实心填充，因为这个控件承担的是「排版工具」的角色，
 * 和数据卡片是两类东西。
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
            Text(
                text = label(p),
                color = if (isSelected) AppColors.Panel else AppColors.TextSub,
                fontSize = AppText.Small,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clickable { onSelect(p) }
                    .background(
                        color = if (isSelected) AppColors.Accent else Color.Transparent,
                        shape = AppShape.Badge,
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .padding(end = if (i < periods.size - 1) AppSpace.Sm else 0.dp),
            )
        }
    }
}
