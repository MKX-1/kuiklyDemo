package com.example.aistock.components

import androidx.compose.runtime.Composable
import com.example.aistock.components.core.HairLine
import com.example.aistock.data.StockItem
import com.example.aistock.data.formatDouble2
import com.example.aistock.data.formatFen
import com.example.aistock.data.formatPctSigned
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppFont
import com.example.aistock.theme.AppShape
import com.example.aistock.theme.AppSpace
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectDragGesturesAfterLongPress
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 单只股票的「行情栏」—— 报刊风重写。
 *
 * 与上一版（圆角卡片）的刻意区别：**去卡片化**。报纸行情版不是一堆
 * 圆角小盒子，而是一栏栏靠**细分隔线**区隔的文字——所以这里没有
 * background/border，行与行之间用一条 1dp 淡墨线（底部 [HairLine]）。
 * 文气来自排版密度与字族对比，不来自容器装饰。
 *
 * 层级（四个维度叠加，不靠字号微调）：
 *   名称 = 衬线 + SemiBold + 浓墨 / 现价 = 衬线 + Bold + 涨跌色 /
 *   代码行业 = 小号 + 淡墨 / 指标 = 等宽小字
 */
@Composable
fun StockCard(item: StockItem, onClick: () -> Unit = {}, onLongClick: () -> Unit = {}) {
    val changeColor = AppColors.ofChange(item.changePct)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 点击热区放在 padding 之前：整行（含左右留白）都可点，
            // 列表里的触摸目标越大越不容易点空。
            .clickable { onClick() }
            // 长按整行拉起 AI 分析抽屉（与点击进详情是两个独立手势）
            .pointerInput(item.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onLongClick() },
                    onDrag = { _, _ -> },
                    onDragEnd = { },
                    onDragCancel = { },
                )
            }
            .padding(horizontal = AppSpace.ScreenEdge, vertical = 10.dp),
    ) {
        // 第一行：名称 + 代码 | 现价 + 涨跌幅
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = AppColors.TextMain,
                    fontSize = AppText.Title,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFont.Serif,
                )
                Text(
                    text = "${item.code} · ${item.industry}",
                    color = AppColors.TextWeak,
                    fontSize = AppText.Tiny,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatFen(item.price),
                    color = changeColor,
                    fontSize = AppText.Price,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFont.Serif,
                )
                Text(
                    text = "${formatFen(item.change)}  ${formatPctSigned(item.changePct)}",
                    color = changeColor,
                    fontSize = AppText.Caption,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // 第二行：关键指标（等宽小字，行情栏的传统排法）
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            StatCell("今开", formatFen(item.open), Modifier.weight(1f))
            StatCell("最高", formatFen(item.high), Modifier.weight(1f))
            StatCell("最低", formatFen(item.low), Modifier.weight(1f))
            StatCell("换手", "${formatDouble2(item.turnover)}%", Modifier.weight(1f))
        }

        // 第三行：动态标签（规则引擎推导，规则与后端逐条一致）
        if (item.tags.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                item.tags.forEach { tag ->
                    TagChip(tag)
                }
            }
        }

        // 第四行：AI 建议行（只有规则评分达到门槛才展示，避免"每只都强烈推荐"）
        if (item.aiProfile.score >= 80) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(AppColors.AccentSoft, AppShape.Card)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "AI",
                    color = AppColors.Panel,
                    fontSize = AppText.Tiny,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(AppColors.Accent, AppShape.Badge)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
                Text(
                    text = "  ${item.aiProfile.action} · ${item.aiProfile.signal} · ${item.aiProfile.score}分",
                    color = AppColors.Accent,
                    fontSize = AppText.Caption,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        // 行间分隔线：报纸行情栏的栏界
        HairLine()
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = AppColors.TextWeak, fontSize = AppText.Micro)
        Text(
            text = value,
            color = AppColors.TextSub,
            fontSize = AppText.Small,
            fontFamily = AppFont.Mono,
        )
    }
}

@Composable
private fun TagChip(text: String) {
    Text(
        text = text,
        color = AppColors.TextSub,
        fontSize = AppText.Tiny,
        modifier = Modifier
            .padding(end = 6.dp)
            .border(1.dp, AppColors.Line, AppShape.Badge)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
