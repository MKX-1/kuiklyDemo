package com.example.aistock.components

import androidx.compose.runtime.Composable
import com.example.aistock.data.ChartSeries
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

/**
 * 分时走势图（Canvas 自绘，不引第三方图表库）。
 *
 * 几个刻意做出的图形学决定，都是为了「一眼可读」：
 *
 * 1. **Y 轴以昨收为中心对称**：上下边界取 `|最高−昨收|` 与 `|最低−昨收|` 的较大者，
 *    这样昨收线正好落在正中间。好处是「开盘涨 1% 收平」这种走势的视觉形状
 *    与「开盘平收涨 1%」完全对称 —— 若按 min~max 铺满，涨跌会被拉伸得看不出幅度。
 * 2. **均价线用中性灰而非强调色**：均价是参考线，不该跟价格线抢注意力；
 *    整个 App 只有一个强调色（青），这里必须让位。
 * 3. **成交量画在下部独立条带**：与价格共享 x 轴但不共享 y 轴，
 *    直接叠在价格上会让人误读成"价格画到这么低"。
 * 4. **只画当日**（分时天然只有一天的数据）：所以不画日期轴，只在底部标三个时点。
 *
 * 取不到数据时**不画任何线** —— 调用方必须先判断 [ChartSeries.points] 非空，
 * 图上一旦出现线条，用户就会当成当日真实走势来读。
 */
@Composable
fun TrendChart(
    series: ChartSeries,
    modifier: Modifier = Modifier,
    chartHeight: Int = 188,
) {
    val prevClose = series.prevClose.takeIf { it > 0 } ?: series.points.first().price
    // 对称范围：以昨收为中心，取最大偏离
    val deviation = max(abs(series.high - prevClose), abs(series.low - prevClose))
        .coerceAtLeast((prevClose * 0.002).toLong())   // 极端平淡的一天也要给个最小幅度，否则线会贴在一起
    val yMax = (prevClose + deviation).toDouble()
    val yMin = (prevClose - deviation).toDouble()

    val last = series.points.last()
    val lineColor = AppColors.ofChange(if (last.price >= prevClose) 1.0 else -1.0)
    val maxPct = (deviation.toDouble() / prevClose.toDouble()) * 100.0

    Column(modifier = modifier) {
        Row {
            // 量价共享 x 轴的画布
            Canvas(modifier = Modifier.weight(1f).height(chartHeight.dp)) {
                val w = size.width
                val h = size.height
                val padL = 4f
                val padR = 4f
                val priceH = h * 0.72f
                val volTop = h * 0.80f
                val volH = h - volTop
                val innerW = w - padL - padR
                val n = series.points.size
                if (n == 0 || innerW <= 0f) return@Canvas

                fun xOf(i: Int): Float =
                    padL + if (n <= 1) 0f else innerW * (i.toFloat() / (n - 1).toFloat())

                fun yOf(price: Long): Float {
                    val ratio = ((yMax - price.toDouble()) / (yMax - yMin)).toFloat()
                    return (ratio * priceH).coerceIn(0f, priceH)
                }

                // ---- 网格：上/中(昨收)/下 三条 ----
                for (k in 0..2) {
                    val y = priceH * (k / 2f)
                    val isBase = k == 1
                    drawLine(
                        color = if (isBase) AppColors.Line else AppColors.GridLine,
                        start = Offset(padL, y),
                        end = Offset(w - padR, y),
                        strokeWidth = 1f,
                    )
                }

                // ---- 价格线 + 线下渐变填充 ----
                val pricePath = Path()
                series.points.forEachIndexed { i, p ->
                    val x = xOf(i)
                    val y = yOf(p.price)
                    if (i == 0) pricePath.moveTo(x, y) else pricePath.lineTo(x, y)
                }
                // 填充路径：沿价格线走完，再兜到价格区底部闭合
                val fillPath = Path()
                fillPath.moveTo(xOf(0), priceH)
                series.points.forEachIndexed { i, p -> fillPath.lineTo(xOf(i), yOf(p.price)) }
                fillPath.lineTo(xOf(n - 1), priceH)
                fillPath.close()
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.20f), lineColor.copy(alpha = 0.0f)),
                    ),
                )
                drawPath(
                    path = pricePath,
                    brush = Brush.linearGradient(listOf(lineColor, lineColor)),
                    style = Stroke(
                        width = 1.8f,
                        cap = StrokeCap.Round,
                    ),
                )

                // ---- 均价线（中性灰，不抢注意力）----
                val avgPath = Path()
                series.points.forEachIndexed { i, p ->
                    val x = xOf(i)
                    val y = yOf(p.avg)
                    if (i == 0) avgPath.moveTo(x, y) else avgPath.lineTo(x, y)
                }
                drawPath(
                    path = avgPath,
                    brush = Brush.linearGradient(listOf(AppColors.TextWeak, AppColors.TextWeak)),
                    style = Stroke(width = 1.1f),
                )

                // ---- 成交量条带：按「与前一点比」着色，与价格同一套涨跌语义 ----
                val barW = (innerW / n).coerceIn(1f, 4f)
                series.points.forEachIndexed { i, p ->
                    val prev = if (i == 0) prevClose else series.points[i - 1].price
                    val up = p.price >= prev
                    val maxVol = series.points.maxOf { it.volume }.coerceAtLeast(1L)
                    val bh = (p.volume.toFloat() / maxVol.toFloat()) * volH
                    if (bh <= 0f) return@forEachIndexed
                    drawLine(
                        color = (if (up) AppColors.Up else AppColors.Down).copy(alpha = 0.45f),
                        start = Offset(xOf(i), volTop + volH - bh),
                        end = Offset(xOf(i), volTop + volH),
                        strokeWidth = barW,
                    )
                }

                // ---- 最新价标记：一条横向短线和端点圆点 ----
                val lastY = yOf(last.price)
                drawLine(
                    color = lineColor.copy(alpha = 0.35f),
                    start = Offset(padL, lastY),
                    end = Offset(w - padR, lastY),
                    strokeWidth = 1f,
                )
                drawCircle(color = lineColor, radius = 2.6f, center = Offset(xOf(n - 1), lastY))
            }

            // ---- 右侧刻度：只标三个关键位置（上/中/下），够用且不糊 ----
            Column(
                modifier = Modifier.width(48.dp).height(chartHeight.dp).padding(end = 2.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                AxisLabel("+%.2f%%".format(maxPct), AppColors.Up)
                AxisLabel("0.00%", AppColors.TextWeak)
                AxisLabel("-%.2f%%".format(maxPct), AppColors.Down)
            }
        }

        // ---- 时间轴：分时只有一天，标三个时点即可（午间休市画成一段，不假装连续）----
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 52.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "09:30", color = AppColors.TextWeak, fontSize = AppText.Micro)
            Text(text = "11:30 / 13:00", color = AppColors.TextWeak, fontSize = AppText.Micro)
            Text(text = "15:00", color = AppColors.TextWeak, fontSize = AppText.Micro)
        }

        // ---- 图例 ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendDot(lineColor)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "价格", color = AppColors.TextSub, fontSize = AppText.Micro)
            Spacer(modifier = Modifier.width(12.dp))
            LegendDot(AppColors.TextWeak)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "均价", color = AppColors.TextSub, fontSize = AppText.Micro)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${series.points.size} 个分时点 · ${series.date}",
                color = AppColors.TextWeak,
                fontSize = AppText.Micro,
            )
        }
    }
}

@Composable
private fun AxisLabel(text: String, color: Color) {
    Text(text = text, color = color, fontSize = AppText.Micro, fontWeight = FontWeight.Medium)
}

@Composable
private fun LegendDot(color: Color) {
    Spacer(
        modifier = Modifier
            .width(10.dp)
            .height(2.dp)
            .background(color),
    )
}
