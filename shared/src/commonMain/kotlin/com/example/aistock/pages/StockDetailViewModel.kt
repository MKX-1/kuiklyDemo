package com.example.aistock.pages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.aistock.data.AiAnalysis
import com.example.aistock.data.ChartSeries
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
    var chart by mutableStateOf<ChartSeries?>(null)
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

    fun load() {
        viewModelScope.launch {
            loading = true
            stockFailed = false

            val item = api.fetchStock(code)
            stock = item
            stockFailed = item == null

            // 行情都取不到就没必要再打另外两个接口了
            if (item != null) {
                chart = api.fetchChart(token)
                chartUnavailable = chart == null
                analysis = api.fetchAiAnalysis(code)
            }

            loading = false
        }
    }
}
