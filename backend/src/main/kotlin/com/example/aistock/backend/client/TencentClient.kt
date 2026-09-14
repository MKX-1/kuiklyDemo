package com.example.aistock.backend.client

import com.example.aistock.backend.config.Config

/** 上游行情快照抓取接缝：只负责"取回原始文本"，不解析。抽成接口是为了能注入假实现做单测。 */
interface TencentClient {
    /** [query] 形如 "sh600519,hk00700"。 */
    suspend fun fetch(query: String): String
}

class HttpTencentClient(
    private val config: Config,
    private val httpGet: suspend (url: String, timeoutMs: Long) -> String = ::httpGetGbk,
) : TencentClient {
    override suspend fun fetch(query: String): String =
        httpGet(config.tencentQuoteUrl + query, config.tencentTimeoutMs)
}
