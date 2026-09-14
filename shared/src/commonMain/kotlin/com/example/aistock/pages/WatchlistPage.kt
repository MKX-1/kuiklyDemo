package com.example.aistock.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.aistock.components.MarketOverviewBar
import com.example.aistock.components.StockCard
import com.example.aistock.data.DataSource
import com.example.aistock.data.StockApis
import com.example.aistock.data.formatHms
import com.example.aistock.theme.AppColors
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
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.example.aistock.data.Watchlist
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
fun WatchlistScreen(network: () -> NetworkModule, onOpenDetail: (String) -> Unit = {}) {
    // remember：跨重组缓存这个对象，别每次重画都新建一个数据源
    val stockApi = remember { StockApis.stocks(network) }
    val vm: WatchlistViewModel = viewModel { WatchlistViewModel(stockApi) }

    // LaunchedEffect：进入页面时执行一次的副作用（这里是首次拉数据）
    LaunchedEffect(Unit) {
        vm.load()
    }

    // 状态栏高度：宿主 Activity 走的是 edge-to-edge（内容铺满整屏），
    // 所以顶部必须自己让出状态栏，否则标题会和系统的时间/信号图标叠在一起。
    val statusBarHeight = LocalConfiguration.current.statusBarHeight

    Column(modifier = Modifier.fillMaxSize().background(AppColors.PageBg)) {

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

        // ---- 主体列表 ----
        if (vm.loading && vm.stocks.isEmpty()) {
            LoadingBox()
        } else if (vm.stocks.isEmpty()) {
            EmptyBox(onRetry = { vm.load() })
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                vm.overview?.let { overview ->
                    item { MarketOverviewBar(overview) }
                }
                items(items = vm.stocks, key = { it.id }) { stock ->
                    StockCard(stock, onClick = { onOpenDetail(stock.code) })
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
}

/** 数据源角标：实时 / 缓存 / 示例，一眼可辨。 */
@Composable
private fun SourceBadge(source: DataSource, fetchedAt: Long) {
    val (label, color) = when (source) {
        DataSource.LIVE -> "实时" to AppColors.Down
        DataSource.CACHE -> "非实时 ${formatHms(fetchedAt)}" to AppColors.Warning
        DataSource.OFFLINE -> "示例数据" to AppColors.Warning
    }
    Text(
        text = label,
        color = color,
        fontSize = AppText.Tiny,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(AppColors.CardBg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
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
