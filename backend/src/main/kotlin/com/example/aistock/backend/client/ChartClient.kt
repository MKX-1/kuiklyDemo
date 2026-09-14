package com.example.aistock.backend.client

import com.example.aistock.backend.config.Config

/**
 * 走势数据抓取接缝。
 *
 * 与 [TencentClient] 分开成两个接口，而不是塞进一个「大行情 client」：
 * 两者上游不同、编码不同（GBK vs UTF-8）、失败语义也不同
 * （快照失败 = 这次刷新没成功；走势失败只是图上没线，列表照样能看）。
 * 合成一个接口会让调用方不得不处理一个 "只要有一部分失败就算失败" 的模糊返回值。
 */
interface ChartClient {
    /** [token] 形如 sh600519 / hk00700。返回上游原始 JSON 文本（UTF-8）。 */
    suspend fun fetchMinute(token: String): String
}

class HttpChartClient(
    private val config: Config,
    private val httpGet: suspend (url: String, timeoutMs: Long) -> String = ::httpGetUtf8,
) : ChartClient {
    override suspend fun fetchMinute(token: String): String =
        httpGet(config.tencentMinuteUrl + token, config.tencentTimeoutMs)
}
