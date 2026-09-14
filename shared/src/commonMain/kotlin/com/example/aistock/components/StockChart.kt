package com.example.aistock.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.aistock.data.ChartData
import com.example.aistock.data.ChartPeriods
import com.example.aistock.data.ChartWindow
import com.example.aistock.data.formatFen
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppFont
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectTransformGestures
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.drawscope.DrawScope
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 交互式行情图 —— 对标 steamdt 的 CS2 价格走势组件的交互模型：
 *
 *  - **单指横拖**：十字光标吸附到最近的 K 线/分时点，顶部读出行显示该时刻的 OHLC；
 *  - **双指捏合**：以捏合中心为锚点缩放时间跨度（可见根数变，Y 轴按窗口自适应）；
 *  - **＋/－ 按钮**：与双指捏合等价（单指设备与自动化测试也能缩放）；
 *  - **单指拖不平移窗口**：十字与平移两个语义混在一个手指上会打架，
 *    steamdt 的取舍是「单指 = 十字，双指 = 缩放」，这里保持一致。
 *
 * 性能关键点：**只画可见窗口**（[ChartWindow]），300 根日 K 缩到 10 根时
 * 仍然只画 10 根，绘制成本不随数据量涨。
 */
@Composable
fun StockChart(
    data: ChartData,
    modifier: Modifier = Modifier,
) {
    when (data) {
        is ChartData.Minute -> MinuteChart(data, modifier)
        is ChartData.Kline -> KlineChart(data, modifier)
    }
}

// ================================================================ 分时

@Composable
private fun MinuteChart(data: ChartData.Minute, modifier: Modifier) {
    val points = data.series.points
    var crosshair by remember(data) { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        ReadoutRow {
            val i = crosshair
            if (i == null) {
                val last = points.lastOrNull()
                if (last != null) {
                    ReadoutCell("现价", formatFen(last.price), AppColors.ofChange((last.price - data.series.prevClose).toDouble()))
                    ReadoutCell("均价", formatFen(last.avg), AppColors.TextSub)
                    ReadoutCell("时间", last.time, AppColors.TextSub)
                }
            } else {
                points.getOrNull(i)?.let { p ->
                    ReadoutCell("时间", p.time, AppColors.TextSub)
                    ReadoutCell("价", formatFen(p.price), AppColors.ofChange((p.price - data.series.prevClose).toDouble()))
                    ReadoutCell("均", formatFen(p.avg), AppColors.TextSub)
                }
            }
        }

        val n = points.size
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .pointerInput(data.series) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (n < 2) return@detectTransformGestures
                        val slot = size.width / n
                        if (zoom == 1f && pan.x != 0f) {
                            val cur = crosshair ?: n / 2
                            crosshair = (cur + (pan.x / slot).toInt()).coerceIn(0, n - 1)
                        }
                    }
                },
        ) {
            if (n < 2) return@Canvas
            val w = size.width
            val h = size.height
            val volH = h * 0.18f                       // 成交量条带高度
            val priceH = h - volH - 4.dp.toPx()
            val prev = data.series.prevClose
            // Y 轴以昨收为中心对称
            val half = maxOf(data.series.high - prev, prev - data.series.low).coerceAtLeast(1L)
            val yOf = { price: Long ->
                priceH * 0.5f - (price - prev).toFloat() / (2 * half) * (priceH * 0.46f)
            }

            drawGrid(w, h, yOf(prev))
            val last = points.last().price
            drawSmoothLine(points.map { it.price }, yOf, w, AppColors.ofChange((last - prev).toDouble()), 1.6f)
            drawSmoothLine(points.map { it.avg }, yOf, w, AppColors.TextWeak, 1f)

            val maxVol = points.maxOf { it.volume }.coerceAtLeast(1L)
            val slot = w / n
            points.forEachIndexed { i, p ->
                val prevVol = if (i == 0) p else points[i - 1]
                val bh = (p.volume.toFloat() / maxVol) * volH
                drawRect(
                    color = AppColors.ofChange((p.price - prevVol.price).toDouble()).copy(alpha = 0.55f),
                    topLeft = Offset(i * slot, h - bh),
                    size = Size((slot * 0.7f).coerceAtLeast(1f), bh),
                )
            }

            crosshair?.let { i ->
                points.getOrNull(i)?.let { p ->
                    drawCrosshair(xFor(i, n, w), yOf(p.price), w, priceH)
                }
            }
        }

        TimeLabels(
            listOf(
                points.firstOrNull()?.time ?: "",
                points.getOrNull(n / 2)?.time ?: "",
                points.lastOrNull()?.time ?: "",
            ),
        )
    }
}

// ================================================================ K 线

@Composable
private fun KlineChart(data: ChartData.Kline, modifier: Modifier) {
    val candles = data.series.candles
    val total = candles.size
    var window by remember(data) {
        mutableStateOf(ChartWindow.full(total, ChartPeriods.defaultSpan(data.series.period)))
    }
    var crosshair by remember(data) { mutableStateOf<Int?>(null) }
    val bounds = ChartPeriods.spanBounds(data.series.period, total)

    Column(modifier = modifier) {
        ReadoutRow {
            val i = crosshair
            if (i != null) {
                candles.getOrNull(i)?.let { c ->
                    val chg = AppColors.ofChange((c.close - c.open).toDouble())
                    ReadoutCell("时间", c.time, AppColors.TextSub)
                    ReadoutCell("开", formatFen(c.open), chg)
                    ReadoutCell("高", formatFen(c.high), chg)
                    ReadoutCell("低", formatFen(c.low), chg)
                    ReadoutCell("收", formatFen(c.close), chg)
                }
            } else {
                val first = candles.getOrNull(window.start)
                val lastW = candles.getOrNull(window.endExclusive - 1)
                ReadoutCell("区间", "${first?.time ?: "—"} ~ ${lastW?.time ?: "—"}", AppColors.TextSub)
                lastW?.let {
                    ReadoutCell("收", formatFen(it.close), AppColors.ofChange((it.close - it.open).toDouble()))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(AppColors.PanelHi),
        ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data.series) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (total < 2) return@detectTransformGestures
                        if (zoom != 1f) {
                            // 双指：以十字（或窗口中心）为锚缩放
                            val anchor = crosshair ?: (window.start + window.count / 2)
                            val anchorFrac = ((anchor - window.start).toFloat() / window.count).coerceIn(0f, 1f)
                            window = window.zoom(zoom, anchorFrac, total, bounds.first, bounds.last)
                            crosshair = (window.start + (anchorFrac * window.count).toInt())
                                .coerceIn(window.start, window.endExclusive - 1)
                        } else if (pan.x != 0f) {
                            // 单指：拖十字（不平移，语义见类注释）
                            val slot = size.width / window.count
                            val cur = crosshair ?: (window.start + window.count / 2)
                            crosshair = (cur + (pan.x / slot).toInt())
                                .coerceIn(window.start, window.endExclusive - 1)
                        }
                    }
                },
        ) {
            if (total < 2) return@Canvas
            try {
            val w = size.width
            val h = size.height
            val volH = h * 0.18f
            val priceH = h - volH - 4.dp.toPx()

            val visible = candles.subList(window.start, window.endExclusive)
            val hi = visible.maxOf { it.high }
            val lo = visible.minOf { it.low }
            val range = (hi - lo).coerceAtLeast(1L)
            val pad = priceH * 0.06f
            val yOf = { price: Long ->
                pad + (1f - (price - lo).toFloat() / range) * (priceH - 2 * pad)
            }

            drawGrid(w, h, yOf((hi + lo) / 2L))

            val slot = w / window.count
            val bodyW = (slot * 0.62f).coerceAtLeast(1f)
            val maxVol = visible.maxOf { it.volume }.coerceAtLeast(1L)

            visible.forEachIndexed { vi, c ->
                val cx = xFor(vi, window.count, w)   // 注意用窗口内下标：全局下标会让蜡烛画到画布外
                val color = if (c.bullish) AppColors.Up else AppColors.Down
                drawLine(color, Offset(cx, yOf(c.high)), Offset(cx, yOf(c.low)), 1f)
                val top = yOf(maxOf(c.open, c.close))
                val bh = (yOf(minOf(c.open, c.close)) - top).coerceAtLeast(1f)
                drawRect(color = color, topLeft = Offset(cx - bodyW / 2, top), size = Size(bodyW, bh))
                val volH2 = (c.volume.toFloat() / maxVol) * volH
                drawRect(
                    color = color.copy(alpha = 0.55f),
                    topLeft = Offset(cx - bodyW / 2, h - volH2),
                    size = Size(bodyW, volH2),
                )
            }

            crosshair?.let { i ->
                candles.getOrNull(i)?.let { c ->
                    drawCrosshair(xFor(i - window.start, window.count, w), yOf(c.close), w, priceH)
                }
            }

            } catch (e: Throwable) {
                println("[StockChart] kline onDraw 异常: " + e.javaClass.name + ": " + e.message)
                e.stackTraceToString().split('\n').take(15).forEach { println("[StockChart] " + it) }
            }
        }
        TimeLabels(
            listOf(
                candles.getOrNull(window.start)?.time ?: "",
                candles.getOrNull(window.start + window.count / 2)?.time ?: "",
                candles.getOrNull(window.endExclusive - 1)?.time ?: "",
            ),
        )

        // 缩放控件：＋/－ 档位（与双指捏合等价）+ 当前跨度回显
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "跨度",
                color = AppColors.TextWeak,
                fontSize = AppText.Micro,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "－",
                color = if (window.count > bounds.first) AppColors.Accent else AppColors.TextWeak,
                fontSize = AppText.Body,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(enabled = window.count > bounds.first) {
                        window = window.zoom(1f / ZOOM_STEP, 0.5f, total, bounds.first, bounds.last)
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            Text(
                text = "${window.count}根",
                color = AppColors.TextSub,
                fontSize = AppText.Micro,
                fontFamily = AppFont.Mono,
            )
            Text(
                text = "＋",
                color = if (window.count < bounds.last) AppColors.Accent else AppColors.TextWeak,
                fontSize = AppText.Body,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(enabled = window.count < bounds.last) {
                        window = window.zoom(ZOOM_STEP, 0.5f, total, bounds.first, bounds.last)
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}
}

// ================================================================ 绘制原语

private const val ZOOM_STEP = 1.35f

private fun xFor(index: Int, count: Int, width: Float): Float =
    (index + 0.5f) * width / count

/** 横向网格线（两条淡线）+ 中轴基准线。 */
private fun DrawScope.drawGrid(w: Float, h: Float, midY: Float) {
    val line = AppColors.LineSoft
    for (frac in listOf(0.14f, 0.86f)) {
        drawLine(line, Offset(0f, h * frac), Offset(w, h * frac), 1f)
    }
    drawLine(AppColors.Line, Offset(0f, midY), Offset(w, midY), 1.2f)
}

/** 价格/均价折线。 */
private fun DrawScope.drawSmoothLine(
    values: List<Long>,
    yOf: (Long) -> Float,
    w: Float,
    color: Color,
    width: Float,
) {
    if (values.size < 2) return
    val n = values.size
    val path = Path()
    values.forEachIndexed { i, v ->
        val x = xFor(i, n, w)
        val y = yOf(v)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = width))
}

/** 十字光标：细线 + 交点小圆（黛青圆点是指针的「落款」）。 */
private fun DrawScope.drawCrosshair(x: Float, y: Float, w: Float, h: Float) {
    drawLine(AppColors.TextWeak, Offset(x, 0f), Offset(x, h), 1f)
    drawLine(AppColors.TextWeak, Offset(0f, y), Offset(w, y), 1f)
    drawCircle(AppColors.Accent, radius = 3f, center = Offset(x, y))
    drawCircle(AppColors.Panel, radius = 1.6f, center = Offset(x, y))
}

// ================================================================ 小件

@Composable
private fun ReadoutRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun ReadoutCell(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = AppColors.TextWeak, fontSize = AppText.Micro)
        Text(
            text = " " + value,
            color = color,
            fontSize = AppText.Small,
            fontFamily = AppFont.Mono,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TimeLabels(labels: List<String>) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        labels.filter { it.isNotEmpty() }.forEachIndexed { i, label ->
            if (i > 0) Spacer(modifier = Modifier.weight(1f))
            Text(
                text = label,
                color = AppColors.TextWeak,
                fontSize = AppText.Micro,
                fontFamily = AppFont.Mono,
            )
        }
    }
}
