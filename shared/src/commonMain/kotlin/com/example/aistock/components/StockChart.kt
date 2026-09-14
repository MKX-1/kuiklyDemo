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
import com.tencent.kuikly.compose.foundation.gestures.detectHorizontalDragGestures
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
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
 * 交互式行情图 —— 交互模型对标 steamdt 的 CS2 价格走势组件：
 *
 *  - **点击**：选中/取消十字光标，吸附到最近的 K 线/分时点，读出行显示该时刻 OHLC；
 *  - **横向拖动**：十字跟随移动；K 线拖到窗口边缘时窗口跟随平移（能翻看更早/更新的数据）；
 *  - **跨度 ＋/－ 按钮**：缩放时间跨度（可见根数变，Y 轴按窗口自适应）；
 *    捏合手势在本 fork 的 LazyColumn 组合下收不到事件（实测），故不提供。
 *
 * 性能关键点：**只画可见窗口**（[ChartWindow]），绘制成本不随数据量涨。
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

// ================================================================
// 分时（点序列）

@Composable
private fun MinuteChart(data: ChartData.Minute, modifier: Modifier) {
    val points = data.series.points
    var crosshair by remember(data) { mutableStateOf<Int?>(null) }
    val n = points.size

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

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                // 手势：点击选中/取消十字，横向拖动移动十字
                .pointerInput(data.series) {
                    detectTapGestures { offset ->
                        if (n < 2) return@detectTapGestures
                        val i = ((offset.x / size.width) * n).toInt().coerceIn(0, n - 1)
                        crosshair = if (crosshair == i) null else i
                    }
                }
                .pointerInput(data.series) {
                    detectHorizontalDragGestures { change, _ ->
                        if (n < 2) return@detectHorizontalDragGestures
                        crosshair = ((change.position.x / size.width) * n).toInt().coerceIn(0, n - 1)
                    }
                },
        ) {
            if (n < 2) return@Canvas
            val w = size.width
            val h = size.height
            val volH = h * 0.18f
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

// ================================================================
// K 线（蜡烛序列）

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
        // 读出行：十字处 OHLC；无十字时显示窗口区间与最新收盘
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
                .height(280.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    // 手势：点击选中/取消十字；横向拖动移动十字（拖到边缘时窗口跟随平移）
                    .pointerInput(data.series) {
                        detectTapGestures { offset ->
                            if (total < 2) return@detectTapGestures
                            val vi = ((offset.x / size.width) * window.count).toInt().coerceIn(0, window.count - 1)
                            val tapped = window.start + vi
                            crosshair = if (crosshair == tapped) null else tapped
                        }
                    }
                    .pointerInput(data.series) {
                        detectHorizontalDragGestures { change, _ ->
                            if (total < 2) return@detectHorizontalDragGestures
                            val vi = ((change.position.x / size.width) * window.count).toInt().coerceIn(0, window.count - 1)
                            if (vi == 0 && window.start > 0) {
                                window = window.pan(-1, total)
                            } else if (vi == window.count - 1 && window.endExclusive < total) {
                                window = window.pan(1, total)
                            }
                            crosshair = (window.start + vi).coerceIn(window.start, window.endExclusive - 1)
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
                        // 注意 x 用窗口内下标：全局下标会把蜡烛画到画布外
                        val cx = xFor(vi, window.count, w)
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
                    // 防御：上游数据形态变化时图表静默失败比整页崩溃好——但必须留日志
                    println("[StockChart] kline onDraw: " + e.javaClass.name + ": " + e.message)
                }
            }
        }

        // 跨度控件：紧跟成交量条带正下方（＋/－ 与双指捏合等价）+ 当前跨度回显
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

        // 时间轴：窗口首 / 中 / 尾（图例区最底部）
        TimeLabels(
            listOf(
                candles.getOrNull(window.start)?.time ?: "",
                candles.getOrNull(window.start + window.count / 2)?.time ?: "",
                candles.getOrNull(window.endExclusive - 1)?.time ?: "",
            ),
        )
    }
}

// ================================================================
// 绘制原语

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

// ================================================================
// 小件

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
