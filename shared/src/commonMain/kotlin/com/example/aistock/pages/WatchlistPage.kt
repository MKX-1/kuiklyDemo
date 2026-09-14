package com.example.aistock.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.aistock.components.AiSheet
import com.example.aistock.components.core.Badge
import com.example.aistock.components.MarketOverviewBar
import com.example.aistock.components.StockCard
import com.example.aistock.components.core.PeriodSwitcher
import com.example.aistock.data.AiAnalysis
import com.example.aistock.data.DataSource
import com.example.aistock.data.StockApis
import com.example.aistock.data.StockItem
import com.example.aistock.data.Watchlist
import com.example.aistock.data.WatchlistFilter
import com.example.aistock.data.formatHms
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
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.pullToRefreshItem
import com.tencent.kuikly.compose.material3.rememberPullToRefreshState
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.example.aistock.data.StockItem as StockItemModel
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.lifecycle.viewmodel.compose.viewModel

/**
 * 自选行情页 —— 全项目的主屏。
 *
 * 页面类本身很薄：它只是「把 Compose 内容装进 Kuikly 容器」。
 * 真正的内容是下面的 [WatchlistScreen]，一个标准的 @Composable 函数。
 */
@Page("watchlist")
class WatchlistPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        // 注意：模块必须在「真正使用时」才 acquire，不能在 willInit 里急切地取——
        // core 的内置模块（含路由）在 willInit 这个时机还没注册完成（实测踩坑：
        // 提前取会抛 "KRRouterModule 未注册"）。所以这里只把「怎么取」包进 lambda，
        // 跟下面 NetworkModule 的写法保持同一套惰性语义。
        setContent {
            WatchlistScreen(
                network = { acquireModule(NetworkModule.MODULE_NAME) },
                onOpenDetail = { code ->
                    val router = acquireModule(RouterModule.MODULE_NAME) as RouterModule
                    openDetail(router, code)
                },
                onOpenReport = { code ->
                    val router = acquireModule(RouterModule.MODULE_NAME) as RouterModule
                    val pj = JSONObject()
                    pj.put(AiReportPage.PAGE_PARAM_CODE, code)
                    router.openPage(AiReportPage.PAGE_NAME, pj)
                },
            )
        }
    }
}

/**
 * 打开个股详情。
 *
 * 参数走 [JSONObject] 随页面一起传，而不是塞进某个全局单例：
 * 这样详情页可以被任何入口打开（自选列表 / 将来的搜索、推送跳转），
 * 它只依赖"进来的参数"，不依赖"谁调用了我"。
 */
private fun openDetail(router: RouterModule, code: String) {
    val token = Watchlist.tokenOf(code) ?: return
    val params = JSONObject()
    params.put(StockDetailPage.PAGE_PARAM_TOKEN, token)
    router.openPage(StockDetailPage.PAGE_NAME, params)
}

/**
 * 主屏 UI。数据流是单向的：
 *
 *   数据层(StockApi)  →  ViewModel(状态)  →  Composable(渲染)
 *                                              ↓ 用户操作
 *                                            ViewModel(方法)
 *
 * 这就是「单向数据流」：UI 永远不在本地改数据，只调 VM 的方法，由 VM 改状态再驱动 UI 重画。
 * 好处是状态变化只有一个来源，出问题好定位。
 */
@Composable
fun WatchlistScreen(network: () -> NetworkModule, onOpenDetail: (String) -> Unit = {}, onOpenReport: (String) -> Unit = {}) {
    // remember：跨重组缓存这个对象，别每次重画都新建一个数据源
    val stockApi = remember { StockApis.stocks(network) }
    val vm: WatchlistViewModel = viewModel { WatchlistViewModel(stockApi) }

    // AI 抽屉状态：长按卡片拉起；分析数据在抽屉打开时才去取（真实取数时长 = 思考动画时长）
    var sheetItem by remember { mutableStateOf<StockItemModel?>(null) }
    var sheetAnalysis by remember { mutableStateOf<AiAnalysis?>(null) }
    var sheetAnalyzing by remember { mutableStateOf(false) }
    LaunchedEffect(sheetItem?.id) {
        val target = sheetItem ?: return@LaunchedEffect
        sheetAnalyzing = true
        sheetAnalysis = null
        sheetAnalysis = stockApi.fetchAiAnalysis(target.code)
        sheetAnalyzing = false
    }

    // 维度切换：全部 / 沪深 / 港股（纯前端过滤，不重新取数）
    var dimension by remember { mutableStateOf(WatchlistFilter.ALL) }

    // 下拉刷新：isRefreshing 由 vm.loading 驱动（取数开始→转圈，结束→收起）
    val pullState = rememberPullToRefreshState(isRefreshing = vm.loading)
    val listState = rememberLazyListState()

    // LaunchedEffect：进入页面时执行一次的副作用（这里是首次拉数据）
    LaunchedEffect(Unit) {
        vm.load()
    }

    // 状态栏高度：宿主 Activity 走的是 edge-to-edge（内容铺满整屏），
    // 所以顶部必须自己让出状态栏，否则标题会和系统的时间/信号图标叠在一起。
    val statusBarHeight = LocalConfiguration.current.statusBarHeight

    Box(modifier = Modifier.fillMaxSize().background(AppColors.PageBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

        // ---- 顶部 header：标题 + 数据源角标 + AI 摘要文案 ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.HeaderBg)
                .padding(horizontal = 12.dp)
                .padding(top = (statusBarHeight + 12f).dp, bottom = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AI 股票行情",
                    color = AppColors.TextMain,
                    fontSize = AppText.Price,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFont.Serif,
                    modifier = Modifier.weight(1f),
                )
                SourceBadge(vm.dataSource, vm.fetchedAt)
            }
            vm.summary?.let { summary ->
                Text(
                    text = summary.text,
                    color = AppColors.TextSub,
                    fontSize = AppText.Caption,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            // 维度切换：全部 / 沪深 / 港股（复用周期切换条的墨块反白样式）
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PeriodSwitcher(
                    periods = WatchlistFilter.DIMENSIONS,
                    selected = dimension,
                    label = { WatchlistFilter.label(it) },
                    onSelect = { dimension = it },
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            // 报头 double rule：上粗下细两条横线，出版物的签名装置
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(2.dp)
                    .background(AppColors.TextMain),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp)
                    .height(1.dp)
                    .background(AppColors.Line),
            )
        }

        // ---- 状态横幅：如实告诉用户当前数据的可靠性，绝不把示例数据伪装成实时行情 ----
        val banner: String? = when {
            vm.dataSource == DataSource.OFFLINE -> "实时行情暂不可用，当前展示的是示例数据（点此重试）"
            vm.dataSource == DataSource.CACHE -> "行情非实时（更新于 ${formatHms(vm.fetchedAt)}），点此重试"
            vm.missingStock > 0 -> "有 ${vm.missingStock} 只股票行情获取失败，点此重试"
            else -> null
        }
        banner?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .background(AppColors.HeaderBg, RoundedCornerShape(8.dp))
                    .clickable { vm.load() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = it, color = AppColors.Warning, fontSize = AppText.Tiny)
            }
        }

        // ---- 主体列表（含下拉刷新）----
        if (vm.loading && vm.stocks.isEmpty()) {
            LoadingBox()
        } else if (vm.stocks.isEmpty()) {
            EmptyBox(onRetry = { vm.load() })
        } else {
            // 维度过滤：token 前缀判断（sh/sz=沪深，hk=港股）
            val filtered = vm.stocks.filter { WatchlistFilter.matches(dimension, Watchlist.tokenOf(it.code)) }
            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                // 下拉刷新指示器（LazyColumn 第一项）
                pullToRefreshItem(
                    state = pullState,
                    onRefresh = { vm.load() },
                    scrollState = listState,
                ) { progress, refreshing, threshold ->
                    Box(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val hint = when {
                            refreshing -> "刷新中…"
                            progress >= 1f -> "松手刷新"
                            else -> "下拉刷新"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(AppColors.Accent, RoundedCornerShape(2.dp)),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = hint,
                                color = AppColors.TextSub,
                                fontSize = AppText.Small,
                                fontFamily = AppFont.Mono,
                            )
                        }
                    }
                }

                vm.overview?.let { overview ->
                    if (dimension == WatchlistFilter.ALL) {
                        item { MarketOverviewBar(overview) }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "该维度下暂无自选",
                                color = AppColors.TextWeak,
                                fontSize = AppText.Body,
                            )
                        }
                    }
                } else {
                    // 「全部」维度：按市场分组展示（沪深 / 港股 组头）
                    if (dimension == WatchlistFilter.ALL) {
                        val cnList = filtered.filter { WatchlistFilter.matches(WatchlistFilter.CN, Watchlist.tokenOf(it.code)) }
                        val hkList = filtered.filter { WatchlistFilter.matches(WatchlistFilter.HK, Watchlist.tokenOf(it.code)) }
                        if (cnList.isNotEmpty()) {
                            item { GroupHeader(WatchlistFilter.groupTitle(WatchlistFilter.CN, cnList.size)) }
                            items(items = cnList, key = { it.id }) { stock ->
                                StockCard(
                                    stock,
                                    onClick = { onOpenDetail(stock.code) },
                                    onLongClick = { sheetItem = stock },
                                )
                            }
                        }
                        if (hkList.isNotEmpty()) {
                            item { GroupHeader(WatchlistFilter.groupTitle(WatchlistFilter.HK, hkList.size)) }
                            items(items = hkList, key = { it.id + "_hk" }) { stock ->
                                StockCard(
                                    stock,
                                    onClick = { onOpenDetail(stock.code) },
                                    onLongClick = { sheetItem = stock },
                                )
                            }
                        }
                    } else {
                        items(items = filtered, key = { it.id }) { stock ->
                            StockCard(
                                stock,
                                onClick = { onOpenDetail(stock.code) },
                                onLongClick = { sheetItem = stock },
                            )
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "行情数据仅用于功能演示，不构成投资建议",
                            color = AppColors.TextWeak,
                            fontSize = AppText.Tiny,
                        )
                    }
                }
            }
        }
        }

        // ---- AI 抽屉：遮罩 + 底部纸面板 ----
        if (sheetItem != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.TextMain.copy(alpha = 0.35f))
                    .clickable { sheetItem = null },
            )
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                sheetItem?.let { item ->
                    AiSheet(
                        item = item,
                        analysis = sheetAnalysis,
                        analyzing = sheetAnalyzing,
                        onDismiss = { sheetItem = null },
                        onOpenReport = { code ->
                            sheetItem = null
                            onOpenReport(code)
                        },
                    )
                }
            }
        }
    }
}

/** 分组组头：印章方块 + 衬线组名（比 SectionHeader 轻，列表内密集使用）。 */
@Composable
private fun GroupHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.ScreenEdge, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(AppColors.Accent),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = AppColors.TextSub,
            fontSize = AppText.Small,
            fontWeight = FontWeight.SemiBold,
            fontFamily = AppFont.Serif,
        )
    }
}

/** 数据源角标：实时 / 缓存 / 示例，一眼可辨。 */
@Composable
private fun SourceBadge(source: DataSource, fetchedAt: Long) {
    val (label, color) = when (source) {
        DataSource.LIVE -> "实时" to AppColors.Down
        DataSource.CACHE -> "非实时 ${formatHms(fetchedAt)}" to AppColors.Warning
        DataSource.OFFLINE -> "示例数据" to AppColors.Warning
    }
    Badge(
        text = label,
        container = AppColors.CardBg,
        content = color,
        height = 20.dp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "正在获取行情…", color = AppColors.TextSub, fontSize = AppText.Body)
    }
}

@Composable
private fun EmptyBox(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "暂无行情数据", color = AppColors.TextSub, fontSize = AppText.Body)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "点击重试",
                color = AppColors.Primary,
                fontSize = AppText.Body,
                modifier = Modifier.clickable { onRetry() },
            )
        }
    }
}
