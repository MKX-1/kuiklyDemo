package com.example.aistock.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.aistock.components.StockChart
import com.example.aistock.components.core.PeriodSwitcher
import com.example.aistock.data.ChartData
import com.example.aistock.data.ChartPeriods
import com.example.aistock.components.core.AiSpark
import com.example.aistock.components.core.ChevronBack
import com.example.aistock.components.core.EmptyBox
import com.example.aistock.components.core.ErrorBox
import com.example.aistock.components.core.FactorBar
import com.example.aistock.components.core.HairLine
import com.example.aistock.components.core.LoadingBox
import com.example.aistock.components.core.Panel
import com.example.aistock.components.core.SectionHeader
import com.example.aistock.components.core.StatCell
import com.example.aistock.data.AiAnalysis
import com.example.aistock.data.ChartSeries
import com.example.aistock.data.StockApis
import com.example.aistock.data.StockItem
import com.example.aistock.data.formatAmount
import com.example.aistock.data.formatCap
import com.example.aistock.data.formatDouble2
import com.example.aistock.data.formatFen
import com.example.aistock.data.formatPctSigned
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppFont
import com.example.aistock.theme.AppSpace
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.lifecycle.viewmodel.compose.viewModel

/**
 * 个股详情页。
 *
 * 页面类只负责「装容器 + 传参 + 接返回」，内容在 [StockDetailScreen]。
 * 参数从路由带过来（[PAGE_PARAM_TOKEN]），不是全局状态 —— 这样页面可以被任意来源打开
 * （自选列表、搜索结果、将来的推送跳转），不依赖"谁调用了它"。
 */
@Page("stockDetail")
class StockDetailPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        // 在组合之外读一次路由参数：组合可能因状态变化重跑，参数是"进入页面时的事实"
        val token = pagerData.params.optString(PAGE_PARAM_TOKEN)
        setContent {
            StockDetailScreen(
                token = token,
                network = { acquireModule(NetworkModule.MODULE_NAME) },
                onBack = {
                    (acquireModule(RouterModule.MODULE_NAME) as RouterModule).closePage()
                },
            )
        }
    }

    companion object {
        const val PAGE_NAME = "stockDetail"
        const val PAGE_PARAM_TOKEN = "token"
    }
}

@Composable
fun StockDetailScreen(
    token: String,
    network: () -> NetworkModule,
    onBack: () -> Unit,
) {
    val stockApi = remember { StockApis.stocks(network) }
    val vm: StockDetailViewModel = viewModel { StockDetailViewModel(stockApi, token) }

    LaunchedEffect(Unit) {
        if (token.isNotBlank()) vm.load()
    }

    val statusBarHeight = LocalConfiguration.current.statusBarHeight

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PageBg)) {
        DetailTopBar(
            title = vm.stock?.name ?: "个股详情",
            subtitle = vm.stock?.code,
            topPadding = (statusBarHeight + 10f).dp,
            onBack = onBack,
        )

        when {
            vm.loading && vm.stock == null -> LoadingBox(
                text = "正在拉取 ${token.uppercase()} 行情…",
            )

            vm.stockFailed -> ErrorBox(
                message = "行情接口没有响应，或这只标的当前没有数据",
                onRetry = { vm.load() },
            )

            else -> {
                val item = vm.stock
                if (item == null) {
                    EmptyBox("暂无数据")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item { PriceBlock(item) }
                        item { Spacer(modifier = Modifier.height(AppSpace.Sm)); ChartPanel(data = vm.chart, unavailable = vm.chartUnavailable, loading = vm.chartLoading, period = vm.period, onPeriod = vm::switchPeriod) }
                        item { Spacer(modifier = Modifier.height(AppSpace.Sm)); QuoteGrid(item) }
                        vm.analysis?.let { an -> item { Spacer(modifier = Modifier.height(AppSpace.Sm)); AiPanel(an) } }
                        item { Disclaimer() }
                    }
                }
            }
        }
    }
}

/** 顶部栏：返回键 + 名称/代码。深色底上用 1px 分隔线收口，不用阴影。 */
@Composable
private fun DetailTopBar(
    title: String,
    subtitle: String?,
    topPadding: Dp,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.HeaderBg)
            .padding(horizontal = AppSpace.ScreenEdge)
            .padding(top = topPadding, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                ChevronBack(
                    modifier = Modifier.size(width = 9.dp, height = 16.dp),
                    color = AppColors.TextMain,
                )
            }
            Spacer(modifier = Modifier.width(AppSpace.Sm))
            Column {
                Text(text = title, color = AppColors.TextMain, fontSize = AppText.Title, fontWeight = FontWeight.SemiBold, fontFamily = AppFont.Serif)
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle, color = AppColors.TextWeak, fontSize = AppText.Micro)
                }
            }
        }
    }
}

/** 价格块：一屏最大的数字放在这里，涨跌色由「现价 vs 昨收」决定。 */
@Composable
private fun PriceBlock(item: StockItem) {
    val color = AppColors.ofChange(item.changePct)
    val prevClose = item.price - item.change

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.HeaderBg)
            .padding(horizontal = AppSpace.ScreenEdge)
            .padding(bottom = AppSpace.Lg),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatFen(item.price),
                color = color,
                fontSize = AppText.HeroPrice,
                fontWeight = FontWeight.Bold,
                fontFamily = AppFont.Serif,
            )
            Spacer(modifier = Modifier.width(AppSpace.Sm))
            Text(
                text = (if (item.change >= 0) "+" else "") + formatFen(item.change),
                color = color,
                fontSize = AppText.Delta,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatPctSigned(item.changePct),
                color = color,
                fontSize = AppText.Delta,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "昨收 ${formatFen(prevClose)} · ${item.industry}",
            color = AppColors.TextWeak,
            fontSize = AppText.Micro,
        )
    }
}

/**
 * 走势面板：周期切换条 + 交互图（对标 steamdt 的交互模型——
 * 单指拖十字、双指捏合缩放、加减档按钮）。
 * 数据拿不到时明确说"不可用"，而不是留白让人以为是加载中。
 */
@Composable
private fun ChartPanel(
    data: ChartData?,
    unavailable: Boolean,
    loading: Boolean,
    period: String,
    onPeriod: (String) -> Unit,
) {
    Panel(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpace.ScreenEdge)) {
        SectionHeader(
            title = "走势",
            trailing = data?.let { d ->
                when (d) {
                    is ChartData.Minute -> "${d.series.date.take(4)}-${d.series.date.substring(4, 6)}-${d.series.date.substring(6, 8)}"
                    is ChartData.Kline -> d.series.name
                }
            },
        )
        PeriodSwitcher(
            periods = ChartPeriods.ALL,
            selected = period,
            label = { ChartPeriods.label(it) },
            onSelect = onPeriod,
        )
        Spacer(modifier = Modifier.height(AppSpace.Sm))
        when {
            data != null -> StockChart(data = data, modifier = Modifier.fillMaxWidth())
            unavailable -> Box(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "暂无走势数据", color = AppColors.TextSub, fontSize = AppText.Body)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "走势需要自建后端提供（本地降级链不提供该数据）",
                        color = AppColors.TextWeak,
                        fontSize = AppText.Micro,
                    )
                }
            }
            else -> LoadingBox(text = "正在拉取走势…")
        }
        if (loading && data != null) {
            Text(
                text = "切换周期中…",
                color = AppColors.TextWeak,
                fontSize = AppText.Micro,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** 盘口数据：三列网格。数字是主角，标签只是注解。 */
@Composable
private fun QuoteGrid(item: StockItem) {
    Panel(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpace.ScreenEdge)) {
        SectionHeader(title = "盘口数据", accent = false)
        GridRow(
            Triple("今开", formatFen(item.open), AppColors.ofChange((item.open - item.price).toDouble())),
            Triple("最高", formatFen(item.high), AppColors.Up),
            Triple("最低", formatFen(item.low), AppColors.Down),
        )
        Spacer(modifier = Modifier.height(AppSpace.Md))
        GridRow(
            Triple("换手率", "${formatDouble2(item.turnover)}%", AppColors.TextMain),
            Triple("量比", if (item.volumeRatio > 0) formatDouble2(item.volumeRatio) else "—", AppColors.TextMain),
            Triple("振幅", "${formatDouble2(item.amplitude)}%", AppColors.TextMain),
        )
        Spacer(modifier = Modifier.height(AppSpace.Md))
        GridRow(
            Triple("成交额", formatAmount(item.amount), AppColors.TextMain),
            Triple("总市值", formatCap(item.marketCap), AppColors.TextMain),
            Triple("市盈率", if (item.pe > 0) formatDouble2(item.pe) else "—", AppColors.TextMain),
        )
        if (item.benchmarkDelta != null) {
            Spacer(modifier = Modifier.height(AppSpace.Md))
            HairLine()
            Spacer(modifier = Modifier.height(AppSpace.Md))
            GridRow(
                Triple("相对大盘", formatPctSigned(item.benchmarkDelta), AppColors.ofChange(item.benchmarkDelta)),
                Triple("流通市值", formatCap(item.floatCap), AppColors.TextMain),
                Triple("行业", item.industry, AppColors.TextMain),
            )
        }
    }
}

@Composable
private fun GridRow(
    a: Triple<String, String, Color>,
    b: Triple<String, String, Color>,
    c: Triple<String, String, Color>,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        StatCell(label = a.first, value = a.second, valueColor = a.third, modifier = Modifier.weight(1f))
        StatCell(label = b.first, value = b.second, valueColor = b.third, modifier = Modifier.weight(1f))
        StatCell(label = c.first, value = c.second, valueColor = c.third, modifier = Modifier.weight(1f))
    }
}

/** AI 分析块：评分 + 结论 + 因子分解 + 目标价/止损价。 */
@Composable
private fun AiPanel(analysis: AiAnalysis) {
    Panel(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpace.ScreenEdge),
        highlighted = true,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AiSpark(modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "AI 分析", color = AppColors.Accent, fontSize = AppText.Title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${analysis.score} 分",
                color = AppColors.Accent,
                fontSize = AppText.Title,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(AppSpace.Sm))
        Text(text = analysis.trendLabel, color = AppColors.TextMain, fontSize = AppText.Body, fontWeight = FontWeight.SemiBold)
        Text(
            text = analysis.trendText,
            color = AppColors.TextSub,
            fontSize = AppText.Caption,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(modifier = Modifier.height(AppSpace.Md))
        HairLine(accent = true)
        Spacer(modifier = Modifier.height(AppSpace.Md))

        FactorBar(label = "动量", value = analysis.factors.momentum, max = 60)
        Spacer(modifier = Modifier.height(AppSpace.Sm))
        FactorBar(label = "价值", value = analysis.factors.value, max = 25)
        Spacer(modifier = Modifier.height(AppSpace.Sm))
        FactorBar(label = "风险", value = analysis.factors.risk, max = 8, color = AppColors.Warning)

        Spacer(modifier = Modifier.height(AppSpace.Md))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(label = "目标价", value = formatFen(analysis.targetPrice), valueColor = AppColors.Up, modifier = Modifier.weight(1f))
            StatCell(label = "止损价", value = formatFen(analysis.stopLossPrice), valueColor = AppColors.Down, modifier = Modifier.weight(1f))
            StatCell(label = "风险评估", value = analysis.riskLevel, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(AppSpace.Sm))
        Text(text = analysis.riskText, color = AppColors.TextWeak, fontSize = AppText.Micro)
    }
}

@Composable
private fun Disclaimer() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpace.Lg, horizontal = AppSpace.ScreenEdge),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "行情与 AI 结论仅用于功能演示，不构成投资建议",
            color = AppColors.TextWeak,
            fontSize = AppText.Micro,
        )
    }
}
