package com.example.aistock.components

import androidx.compose.runtime.Composable
import com.example.aistock.data.StockItem
import com.example.aistock.data.formatDouble2
import com.example.aistock.data.formatFen
import com.example.aistock.data.formatPctSigned
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 单只股票卡片（可复用 UI 组件）。
 *
 * @Composable 函数就是「组件」：输入参数 → 输出 UI。它没有返回值，
 * 而是在组合树里"描述"该画什么；参数一变，Compose 自动重画发生变化的部分。
 *
 * 这个组件只负责「怎么显示一只股票」，不关心数据从哪来、点击后去哪——
 * 这样它能被自选页、搜索结果页、详情页反复使用。
 */
@Composable
fun StockCard(item: StockItem) {
    val changeColor = AppColors.ofChange(item.changePct)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .background(AppColors.CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                )
                Text(
                    text = "${item.code} · ${item.industry}",
                    color = AppColors.TextSub,
                    fontSize = AppText.Tiny,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatFen(item.price),
                    color = changeColor,
                    fontSize = AppText.Price,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${formatFen(item.change)}  ${formatPctSigned(item.changePct)}",
                    color = changeColor,
                    fontSize = AppText.Caption,
                )
            }
        }

        // 第二行：关键指标
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
                    .background(AppColors.HeaderBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "AI",
                    color = Color.White,
                    fontSize = AppText.Tiny,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(AppColors.AiAccent, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
                Text(
                    text = "  ${item.aiProfile.action} · ${item.aiProfile.signal} · ${item.aiProfile.score}分",
                    color = AppColors.TextMain,
                    fontSize = AppText.Caption,
                )
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = AppColors.TextWeak, fontSize = AppText.Tiny)
        Text(text = value, color = AppColors.TextMain, fontSize = AppText.Caption)
    }
}

@Composable
private fun TagChip(text: String) {
    Text(
        text = text,
        color = AppColors.Primary,
        fontSize = AppText.Tiny,
        modifier = Modifier
            .padding(end = 6.dp)
            .background(AppColors.HeaderBg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
