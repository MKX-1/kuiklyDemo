package com.example.aistock.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.aistock.components.core.FactorBar
import com.example.aistock.components.core.HairLine
import com.example.aistock.components.core.SectionHeader
import com.example.aistock.data.AiAnalysis
import com.example.aistock.data.AiNarrative
import com.example.aistock.data.StockApi
import com.example.aistock.data.StockApis
import com.example.aistock.data.StockItem
import com.example.aistock.data.formatFen
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppFont
import com.example.aistock.theme.AppShape
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
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.lifecycle.ViewModel
import com.tencent.kuikly.lifecycle.viewModelScope
import com.tencent.kuikly.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/**
 * AI 分析报告页（路由 aiReport，参数 code）。
 *
 * 与弹层的分工：弹层是「结论速览」（因子 + 三段叙事），报告页是**完整论证**——
 * 评分构成、三段详述、目标/止损与风险声明，信息量递增。
 * 数据走同一条降级链（后端分析 → 本地规则引擎），页面不感知来源差异。
 */
@Page("aiReport")
class AiReportPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        val code = pagerData.params.optString(PAGE_PARAM_CODE)
        setContent {
            AiReportScreen(
                code = code,
                network = { acquireModule(NetworkModule.MODULE_NAME) },
                onBack = {
                    (acquireModule(RouterModule.MODULE_NAME) as RouterModule).closePage()
                },
            )
        }
    }

    companion object {
        const val PAGE_NAME = "aiReport"
        const val PAGE_PARAM_CODE = "code"
    }
}

/** 报告页状态：行情 + 分析各自独立成败（同详情页原则——次要数据失败不拖垮整页）。 */
class AiReportViewModel(
    private val api: StockApi,
    private val code: String,
) : ViewModel() {

    var stock by mutableStateOf<StockItem?>(null)
        private set
    var analysis by mutableStateOf<AiAnalysis?>(null)
        private set
    var loading by mutableStateOf(true)
        private set

    fun load() {
        viewModelScope.launch {
            loading = true
            stock = api.fetchStock(code)
            analysis = api.fetchAiAnalysis(code)
            loading = false
        }
    }
}

@Composable
fun AiReportScreen(
    code: String,
    network: () -> NetworkModule,
    onBack: () -> Unit,
) {
    val stockApi = remember { StockApis.stocks(network) }
    val vm: AiReportViewModel = viewModel { AiReportViewModel(stockApi, code) }

    LaunchedEffect(code) {
        if (code.isNotBlank()) vm.load()
    }

    val statusBarHeight = LocalConfiguration.current.statusBarHeight

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PageBg)) {
        // 顶栏：返回 + 标题
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.HeaderBg)
                .padding(horizontal = AppSpace.ScreenEdge)
                .padding(top = (statusBarHeight + 10f).dp, bottom = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "‹",
                    color = AppColors.TextMain,
                    fontSize = AppText.Title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onBack() },
                )
                Spacer(modifier = Modifier.width(AppSpace.Sm))
                Column {
                    Text(
                        text = "AI 分析报告",
                        color = AppColors.TextMain,
                        fontSize = AppText.Title,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AppFont.Serif,
                    )
                    vm.stock?.let {
                        Text(
                            text = "${it.name} · ${it.code}",
                            color = AppColors.TextWeak,
                            fontSize = AppText.Micro,
                        )
                    }
                }
            }
        }

        when {
            vm.loading && vm.analysis == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "正在生成报告…", color = AppColors.TextSub, fontSize = AppText.Body)
            }

            vm.analysis == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "报告生成失败，返回重试",
                    color = AppColors.TextSub,
                    fontSize = AppText.Body,
                )
            }

            else -> {
                val item = vm.stock
                val analysis = vm.analysis
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (item != null && analysis != null) {
                        // 评分块：一屏最大的数字
                        item {
                            ScoreBlock(item, analysis)
                        }
                        // 评分构成
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpace.ScreenEdge, vertical = AppSpace.Sm),
                            ) {
                                SectionHeader(title = "评分构成")
                                Spacer(modifier = Modifier.height(AppSpace.Xs))
                                FactorBar(label = "动量（近一日涨跌幅与量比）", value = analysis.factors.momentum, max = 60)
                                Spacer(modifier = Modifier.height(AppSpace.Sm))
                                FactorBar(label = "价值（市盈率维度）", value = analysis.factors.value, max = 25)
                                Spacer(modifier = Modifier.height(AppSpace.Sm))
                                FactorBar(label = "风险（振幅维度，越高越稳）", value = analysis.factors.risk, max = 8, color = AppColors.Warning)
                            }
                        }
                        // 三段详述
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpace.ScreenEdge, vertical = AppSpace.Sm),
                            ) {
                                SectionHeader(title = "趋势详述")
                                ReportParagraph(AiNarrative.trend(item, analysis))
                                SectionHeader(title = "风险详述")
                                ReportParagraph(AiNarrative.risk(item, analysis))
                                SectionHeader(title = "操作建议")
                                ReportParagraph(AiNarrative.action(item, analysis))
                            }
                        }
                        // 关键价位
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpace.ScreenEdge, vertical = AppSpace.Sm),
                            ) {
                                SectionHeader(title = "关键价位", accent = false)
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    com.example.aistock.components.core.StatCell(
                                        label = "现价",
                                        value = formatFen(item.price),
                                        valueColor = AppColors.ofChange(item.changePct),
                                        modifier = Modifier.weight(1f),
                                    )
                                    com.example.aistock.components.core.StatCell(
                                        label = "目标价",
                                        value = formatFen(analysis.targetPrice),
                                        valueColor = AppColors.Up,
                                        modifier = Modifier.weight(1f),
                                    )
                                    com.example.aistock.components.core.StatCell(
                                        label = "止损价",
                                        value = formatFen(analysis.stopLossPrice),
                                        valueColor = AppColors.Down,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        // 免责声明
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = AppSpace.Lg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (analysis.source == "llm") "本报告由大模型基于当日行情生成，仅供参考，不构成投资建议" else "本报告由规则引擎自动生成，仅供参考，不构成投资建议",
                                    color = AppColors.TextWeak,
                                    fontSize = AppText.Micro,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 报告头：名称/行业 + 大号衬线评分 + 风险标签。 */
@Composable
private fun ScoreBlock(item: StockItem, analysis: AiAnalysis) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.HeaderBg)
            .padding(horizontal = AppSpace.ScreenEdge)
            .padding(top = AppSpace.Md, bottom = AppSpace.Lg),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${analysis.score}",
                color = AppColors.Accent,
                fontSize = AppText.HeroPrice,
                fontWeight = FontWeight.Bold,
                fontFamily = AppFont.Serif,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "分 / 100",
                color = AppColors.TextSub,
                fontSize = AppText.Caption,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = item.aiProfile.action,
                color = AppColors.TextMain,
                fontSize = AppText.Title,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppFont.Serif,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        HairLine()
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = analysis.trendLabel + " · " + analysis.riskLevel + " · " + item.industry,
            color = AppColors.TextSub,
            fontSize = AppText.Caption,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 来源如实标注：llm=大模型 / rules=规则引擎兜底
        Text(
            text = if (analysis.source == "llm") "结论来源：大模型（qwen）" else "结论来源：规则引擎",
            color = AppColors.Accent,
            fontSize = AppText.Micro,
        )
    }
}

/** 报告段落正文。 */
@Composable
private fun ReportParagraph(text: String) {
    Text(
        text = text,
        color = AppColors.TextSub,
        fontSize = AppText.Caption,
        modifier = Modifier.padding(bottom = AppSpace.Md),
    )
}
