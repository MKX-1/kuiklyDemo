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

    /**
     * K 线。[period] ∈ m60 / day / week / month。
     * 日/周/月走 fqkline（前复权），分钟级走 mkline——两套接口路径不同。
     */
    suspend fun fetchKline(token: String, period: String): String
}

/** 各周期取的数据根数：跨度要在「缩放到最细」和「请求体积」之间取平衡。 */
private val KLINE_COUNT = mapOf("m60" to 240, "day" to 320, "week" to 160, "month" to 120)

class HttpChartClient(
    private val config: Config,
    private val httpGet: suspend (url: String, timeoutMs: Long) -> String = ::httpGetUtf8,
) : ChartClient {
    override suspend fun fetchMinute(token: String): String =
        httpGet(config.tencentMinuteUrl + token, config.tencentTimeoutMs)

    override suspend fun fetchKline(token: String, period: String): String {
        val count = KLINE_COUNT[period] ?: throw IllegalArgumentException("unsupported period: $period")
        val url = if (period == "m60") {
            config.tencentMklineUrl + "$token,m60,,$count"
        } else {
            // fqkline 参数槽位：token,period,开始,结束,数量,复权方式
            // 开始/结束留空 → 注意是**三个逗号**（period 后两个空槽各占一个），少一个就 param error
            config.tencentKlineUrl + "$token,$period,,,$count,qfq"
        }
        println("[chart] GET $url")
        return httpGet(url, config.tencentTimeoutMs)
    }
}
