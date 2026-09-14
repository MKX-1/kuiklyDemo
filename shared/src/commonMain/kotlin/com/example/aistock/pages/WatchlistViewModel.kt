package com.example.aistock.pages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.aistock.data.AiSummary
import com.example.aistock.data.DataSource
import com.example.aistock.data.MarketOverview
import com.example.aistock.data.SampleStockApi
import com.example.aistock.data.StockApi
import com.example.aistock.data.StockItem
import com.example.aistock.data.WatchlistBundle
import com.example.aistock.data.deriveMarketOverview
import com.example.aistock.data.deriveSummary
import com.tencent.kuikly.lifecycle.ViewModel
import com.tencent.kuikly.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * 自选行情页的 ViewModel（MVVM 里的 VM 层）。
 *
 * 职责边界：只做「加载数据 + 维护状态」，不碰任何 UI 概念（不持有 Activity、不做路由、
 * 不弹 toast）。这样它才好测试，也能被未来的 iOS/鸿蒙页面直接复用。
 *
 * [api] 由页面注入（因为它需要 NetworkModule，属于 UI 层的东西）。
 */
class WatchlistViewModel(private val api: StockApi) : ViewModel() {

    // —— 列表状态。用 mutableStateOf 包起来：值一变，读它的 Composable 会自动重组（刷新 UI）——
    var stocks by mutableStateOf<List<StockItem>>(emptyList())
        private set
    var summary by mutableStateOf<AiSummary?>(null)
        private set
    var dataSource by mutableStateOf(DataSource.OFFLINE)
        private set
    var fetchedAt by mutableStateOf(0L)
        private set
    var missingStock by mutableStateOf(0)
        private set
    var loading by mutableStateOf(true)
        private set
    var offlineNotice by mutableStateOf(false)   // 是否正在展示示例数据
        private set

    /** 大盘摘要：与列表同源（同一次拉取的 stocks 推导），刷新后一起更新。 */
    val overview: MarketOverview?
        get() = if (stocks.isEmpty()) null else deriveMarketOverview(stocks, fetchedAt)

    /**
     * 首次进入 / 点重试时调用。
     */
    fun load() {
        viewModelScope.launch {
            loading = true
            applyFetch(api.fetchWatchlist())
            loading = false
        }
    }

    /** 下拉刷新：不显示整屏 loading，避免打断阅读。 */
    fun refresh() {
        viewModelScope.launch {
            applyFetch(api.fetchWatchlist())
        }
    }

    /**
     * 把一次拉取结果落到状态上。
     *
     * 这里处理一个容易忽略的真实场景：**刷新失败但有旧数据**。
     * 此时不该把列表清空，而是保留旧数据、把来源盖章成 CACHE，
     * 让 UI 显示「行情非实时（更新于 10:32:11）」——用户能感知数据年龄，而不是看到一片空白。
     */
    private fun applyFetch(bundle: WatchlistBundle) {
        if (bundle.stocks.isEmpty() && stocks.isNotEmpty()) {
            // 本轮没拿到新数据，沿用上一次的，并标成缓存态
            dataSource = DataSource.CACHE
            return
        }
        stocks = bundle.stocks
        fetchedAt = bundle.fetchedAt
        dataSource = bundle.source
        missingStock = bundle.missing
        offlineNotice = bundle.source == DataSource.OFFLINE
        summary = bundle.summary.takeIf { it.text.isNotBlank() }
            ?: deriveSummary(bundle.stocks, bundle.fetchedAt)
    }

    /** 供离线兜底兜底（数据层已处理，这里只在极端情况下用）。 */
    suspend fun fallback(): WatchlistBundle = SampleStockApi.fetchWatchlist()
}
