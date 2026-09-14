package com.example.aistock.backend

import com.example.aistock.backend.ai.LlmAiEngine
import com.example.aistock.backend.ai.RuleEngineAiEngine
import com.example.aistock.backend.client.HttpTencentClient
import com.example.aistock.backend.config.Config
import com.example.aistock.backend.route.modules
import com.example.aistock.backend.service.WatchlistService
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/**
 * 后端入口。
 *
 * 依赖装配的写法是"手工依赖注入"：在 main 里把实现组装起来传进去。
 * 好处是依赖关系一目了然，也不需要引入 Spring 那套容器。
 */
fun main() {
    val config = Config.load()
    val client = HttpTencentClient(config)
    // LLM 优先、规则兜底：key 缺失时自动退化为纯规则引擎（同一接口，调用方无感）
    val engine = if (config.llmApiKey.isNotBlank()) {
        println("AI 引擎: LlmAiEngine（大模型 " + config.llmModel + "，规则引擎兜底）")
        LlmAiEngine(config, RuleEngineAiEngine)
    } else {
        println("AI 引擎: RuleEngineAiEngine（未配置 DASHSCOPE_API_KEY）")
        RuleEngineAiEngine
    }
    val service = WatchlistService(client, engine)

    println("AI 股票行情后端已启动: http://127.0.0.1:${config.port}")
    println("  探活: GET /health")
    println("  行情: GET /watchlist?codes=sh600519,hk00700")
    println("  分析: GET /analysis/sh600519")

    embeddedServer(Netty, port = config.port) {
        modules(service)
    }.start(wait = true)
}
