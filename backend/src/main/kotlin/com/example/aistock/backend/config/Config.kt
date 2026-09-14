package com.example.aistock.backend.config

/**
 * 服务配置：默认值写死在代码里，可被环境变量覆盖。
 * 这样本地 `./gradlew run` 直接能跑，部署时又不用改代码。
 */
data class Config(
    val port: Int,
    val tencentQuoteUrl: String,
    val tencentMinuteUrl: String,
    val tencentKlineUrl: String,
    val tencentMklineUrl: String,
    val tencentTimeoutMs: Long,
) {
    companion object {
        fun load(env: Map<String, String> = System.getenv()): Config = Config(
            port = (env["PORT"] ?: "8080").toIntOrNull() ?: 8080,
            tencentQuoteUrl = env["TENCENT_QUOTE_URL"] ?: "http://qt.gtimg.cn/q=",
            // 分时走势：注意**不能**用老的 data.gtimg.cn/flashdata/*，
            // 那套接口现在返回的是过期快照（实测给到 2021 年），画出来就是陈年行情。
            // web.ifzq.gtimg.cn 这套 JSON 接口才是当日的。
            tencentMinuteUrl = env["TENCENT_MINUTE_URL"]
                ?: "https://web.ifzq.gtimg.cn/appstock/app/minute/query?code=",
            // 日/周/月 K（前复权）：param={token},{period},,,{数量},qfq
            tencentKlineUrl = env["TENCENT_KLINE_URL"]
                ?: "https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?param=",
            // 分钟级 K 线（m60 等）：param={token},m60,,{数量}
            tencentMklineUrl = env["TENCENT_MKLINE_URL"]
                ?: "https://ifzq.gtimg.cn/appstock/app/kline/mkline?param=",
            tencentTimeoutMs = (env["TENCENT_TIMEOUT_MS"] ?: "5000").toLongOrNull() ?: 5000L,
        )
    }
}
