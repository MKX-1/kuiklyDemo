package com.example.aistock.pages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.aistock.data.AiAnalysis
import com.example.aistock.data.ChartData
import com.example.aistock.data.ChartPeriods
import com.example.aistock.data.StockApi
import com.example.aistock.data.StockItem
import com.tencent.kuikly.lifecycle.ViewModel
import com.tencent.kuikly.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * 个股详情页的 ViewModel。
 *
 * 关键设计：**三个数据各自独立成败**。
 * 行情、走势、AI 分析来自三个不同接口，任何一个挂掉都不该让整页变空白：
 *  - 行情拿不到 → 整页无法成立，走失败态 + 重试；
 *  - 走势拿不到 → 只是图上没线，其它照常（且**绝不画假线**）；
 *  - 分析拿不到 → 隐藏分析块。
 *
 * 如果把它们揉成一个 "全部成功才渲染" 的 loading，用户会因为一个次要接口超时而看不到价格。
 */
class StockDetailViewModel(
    private val api: StockApi,
    private val token: String,
) : ViewModel() {

    var stock by mutableStateOf<StockItem?>(null)
        private set

    /** 当前周期的图表数据。 */
    var chart by mutableStateOf<ChartData?>(null)
        private set

    /** 当前选中周期（分时 / 60分 / 日K / 周K / 月K）。 */
    var period by mutableStateOf(ChartPeriods.DAY)
        private set

    /** 切换周期时的加载态（只影响图区，不盖住整页）。 */
    var chartLoading by mutableStateOf(false)
        private set

    var analysis by mutableStateOf<AiAnalysis?>(null)
        private set
    var loading by mutableStateOf(true)
        private set

    /** 行情失败（决定是否显示整页失败态）。 */
    var stockFailed by mutableStateOf(false)
        private set

    /** 走势不可用（只是图区显示空态，不影响页面其它部分）。 */
    var chartUnavailable by mutableStateOf(false)
        private set

    private val code: String = token.removePrefix("sh").removePrefix("sz").removePrefix("hk")

    /** 已取过的周期缓存：切回已看过的周期不发请求（数据在会话内足够新鲜）。 */
    private val cache = mutableMapOf<String, ChartData>()

    fun load() {
        viewModelScope.launch {
            loading = true
            stockFailed = false

            val item = api.fetchStock(code)
            stock = item
            stockFailed = item == null

            // 行情都取不到就没必要再打另外两个接口了
            if (item != null) {
                loadChart(period)
                analysis = api.fetchAiAnalysis(code)
            }

            loading = false
        }
    }

    /**
     * 切换周期。已缓存则即时切换（不发请求），否则拉取。
     * 切换期间旧图保持显示（chartLoading 只是让切换条转圈），比闪白屏体验好。
     */
    fun switchPeriod(newPeriod: String) {
        if (newPeriod == period) return
        period = newPeriod
        cache[newPeriod]?.let {
            chart = it
            chartUnavailable = false
            return
        }
        viewModelScope.launch {
            chartLoading = true
            val data = api.fetchChart(token, newPeriod)
            if (data != null) cache[newPeriod] = data
            chart = data
            chartUnavailable = data == null
            chartLoading = false
        }
    }

    private suspend fun loadChart(p: String) {
        chartLoading = true
        val data = api.fetchChart(token, p)
        if (data != null) cache[p] = data
        chart = data
        chartUnavailable = data == null
        chartLoading = false
    }
}
