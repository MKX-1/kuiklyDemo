package com.example.aistock.backend.service

import com.example.aistock.backend.client.ChartClient
import com.example.aistock.backend.client.HttpChartClient
import com.example.aistock.backend.config.Config
import com.example.aistock.backend.dto.ChartDto
import com.example.aistock.backend.dto.ChartPointDto
import com.example.aistock.backend.parse.MinuteParser
import java.io.IOException

/**
 * 走势服务：编排「取数 → 解析 → 补均价 → 组装契约」。
 *
 * 与前端的职责切分和行情一致：**上游那套别扭的字符串格式只在这里被理解一次**，
 * 客户端拿到的是干净的点数组，不需要知道腾讯把数组写成了字符串字面量这件事。
 */
class ChartService(private val client: ChartClient) {

    /**
     * 分时走势。返回 null 表示"上游答了，但这份数据不可用"（格式变了 / 当日无数据），
     * 路由层据此回 404；上游**连不上**则抛 [UpstreamUnavailable]，路由层回 502。
     * 这两种情况分开，排障时才能一眼看出是"我们的解析坏了"还是"网断了"。
     */
    suspend fun fetchMinute(token: String): ChartDto? {
        val raw = try {
            client.fetchMinute(token)
        } catch (e: IOException) {
            throw UpstreamUnavailable("tencent minute fetch failed: ${e.message}", e)
        }

        val series = MinuteParser.parse(raw, token) ?: return null

        // 均价线：当日累计成交额 ÷ 累计成交量。
        // 单位换算说明：price 是「分」，volume 是「手」，所以 Σ(volume×price)/Σvolume 直接就是「分」，
        // 不需要再去乘 100 —— 这也是全项目价格统一用分的好处之一。
        var cumVolume = 0L
        var cumAmount = 0L
        val points = series.points.map { p ->
            cumVolume += p.volume
            cumAmount += p.volume * p.price
            ChartPointDto(
                time = p.time,
                price = p.price,
                avg = if (cumVolume > 0) cumAmount / cumVolume else p.price,
                volume = p.volume,
            )
        }

        return ChartDto(
            token = token,
            code = codeOf(token),
            name = series.name.ifBlank { codeOf(token) },
            period = PERIOD_MINUTE,
            date = series.date,
            prevClose = series.prevClose,
            points = points,
        )
    }

    private fun codeOf(token: String): String = token.removePrefix("sh").removePrefix("sz").removePrefix("hk")

    companion object {
        const val PERIOD_MINUTE = "minute"

        /** 生产装配：默认实现打真实上游。测试里换成假 client。 */
        fun create(config: Config = Config.load()): ChartService =
            ChartService(HttpChartClient(config))
    }
}
