package com.example.aistock.backend.config

/**
 * 服务配置：默认值写死在代码里，可被环境变量覆盖。
 * 这样本地 `./gradlew run` 直接能跑，部署时又不用改代码。
 */
data class Config(
    val port: Int,
    val tencentQuoteUrl: String,
    val tencentTimeoutMs: Long,
) {
    companion object {
        fun load(env: Map<String, String> = System.getenv()): Config = Config(
            port = (env["PORT"] ?: "8080").toIntOrNull() ?: 8080,
            tencentQuoteUrl = env["TENCENT_QUOTE_URL"] ?: "http://qt.gtimg.cn/q=",
            tencentTimeoutMs = (env["TENCENT_TIMEOUT_MS"] ?: "5000").toLongOrNull() ?: 5000L,
        )
    }
}
