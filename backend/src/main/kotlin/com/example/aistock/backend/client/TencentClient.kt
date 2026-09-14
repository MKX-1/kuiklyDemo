package com.example.aistock.backend.client

import com.example.aistock.backend.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

/** 上游行情抓取接缝：只负责"取回原始文本"，不解析。抽成接口是为了能注入假实现做单测。 */
interface TencentClient {
    /** [query] 形如 "sh600519,hk00700"。 */
    suspend fun fetch(query: String): String
}

class HttpTencentClient(
    private val config: Config,
    private val httpGet: suspend (url: String, timeoutMs: Long) -> String = ::defaultHttpGet,
) : TencentClient {
    override suspend fun fetch(query: String): String = httpGet(config.tencentQuoteUrl + query, config.tencentTimeoutMs)
}

/** 默认实现：JDK 自带的 HttpURLConnection，够用且零依赖。 */
private suspend fun defaultHttpGet(url: String, timeoutMs: Long): String = withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = timeoutMs.toInt()
    conn.readTimeout = timeoutMs.toInt()
    conn.setRequestProperty("Referer", "https://gu.qq.com/")
    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
    if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
    // ⚠️ 这个接口返回的是 GBK 编码（股票名是中文），用 UTF-8 读会变乱码。
    // 客户端做这件事很麻烦（各端解码能力不一致），放在后端做最省事。
    conn.inputStream.bufferedReader(Charset.forName("GBK")).readText()
}
