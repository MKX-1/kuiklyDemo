package com.example.aistock.backend.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

/**
 * 上游抓取的公共实现。
 *
 * 抽出来的理由：本项目要打两类上游接口，**编码不一样**——
 *  - 行情快照 `qt.gtimg.cn` 是 **GBK**（股票名是中文，用 UTF-8 读会变乱码）；
 *  - 走势 JSON `web.ifzq.gtimg.cn` 是 **UTF-8**。
 *
 * 把「请求 + 超时 + 头 + 解码」收敛到一处，两个 client 只声明自己的编码，
 * 免得同一种请求逻辑写两遍（那种代码必然会有一边忘了设超时）。
 *
 * **必须手动跟随重定向**（实测踩坑）：上游会把 http 302 到 https，
 * 而 HttpURLConnection 默认**不跟随跨协议重定向**——不处理的话拿到的是空 302，
 * 上层只会看到一句干巴巴的 "HTTP 302"，根本想不到是重定向。
 */
internal suspend fun httpGet(url: String, timeoutMs: Long, charsetName: String): String =
    withContext(Dispatchers.IO) {
        var current = url
        var hopsLeft = 5
        while (true) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.connectTimeout = timeoutMs.toInt()
            conn.readTimeout = timeoutMs.toInt()
            conn.instanceFollowRedirects = false   // 自己跟，才能跨协议
            // 上游对没有 Referer 的请求会拒（或给空），这两个头是必需的
            conn.setRequestProperty("Referer", "https://gu.qq.com/")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            try {
                when (val code = conn.responseCode) {
                    in 300..399 -> {
                        val location = conn.getHeaderField("Location")
                            ?: throw IOException("HTTP $code 重定向但没有 Location")
                        // 相对路径的 Location 也要能拼（按 RFC 3986 以上一跳为基准）
                        current = URL(URL(current), location).toString()
                        if (--hopsLeft <= 0) throw IOException("重定向次数超过上限")
                    }
                    200 -> return@withContext conn.inputStream
                        .bufferedReader(Charset.forName(charsetName)).readText()
                    else -> throw IOException("HTTP $code")
                }
            } finally {
                conn.disconnect()
            }
        }
        @Suppress("UNREACHABLE_CODE")
        throw IOException("unreachable")
    }

/** 行情快照：GBK。 */
internal suspend fun httpGetGbk(url: String, timeoutMs: Long): String =
    httpGet(url, timeoutMs, "GBK")

/** 走势 JSON：UTF-8。 */
internal suspend fun httpGetUtf8(url: String, timeoutMs: Long): String =
    httpGet(url, timeoutMs, "UTF-8")
