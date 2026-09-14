package com.example.aistock.components

import androidx.compose.runtime.Composable
import com.example.aistock.data.MarketOverview
import com.example.aistock.data.formatCap
import com.example.aistock.data.formatPctSigned
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppFont
import com.example.aistock.theme.AppShape
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 大盘摘要条（自选池口径）。
 *
 * 注意口径：这里的「市值变动」是 **Σ 流通市值变动**，不是资金净流入。
 * 金融数据最忌讳口径说不清——同一个数字换个口径含义就完全不同。
 */
@Composable
fun MarketOverviewBar(overview: MarketOverview) {
    val total = (overview.up + overview.down + overview.flat).coerceAtLeast(1)
    val upWeight = overview.up.toFloat() / total
    val avgColor = AppColors.ofChange(overview.avgChangePct)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .background(AppColors.Panel, AppShape.Card)
            .border(1.dp, AppColors.Line, AppShape.Card)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "自选概览",
                color = AppColors.TextMain,
                fontSize = AppText.Body,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppFont.Serif,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "平均 ${formatPctSigned(overview.avgChangePct)}",
                color = avgColor,
                fontSize = AppText.Caption,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 涨跌家数：红条（涨） / 绿条（跌），宽度按比例
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(upWeight.coerceIn(0.02f, 0.98f))
                    .height(4.dp)
                    .background(AppColors.Up, RoundedCornerShape(2.dp)),
            )
            Box(
                modifier = Modifier
                    .weight(1f - upWeight.coerceIn(0.02f, 0.98f))
                    .height(4.dp)
                    .background(AppColors.Down, RoundedCornerShape(2.dp)),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "涨 ${overview.up}",
                color = AppColors.Up,
                fontSize = AppText.Tiny,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "跌 ${overview.down}",
                color = AppColors.Down,
                fontSize = AppText.Tiny,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "流通市值变动 ${formatCap(overview.floatCapDelta)}",
                color = AppColors.TextSub,
                fontSize = AppText.Tiny,
            )
        }
    }
}
