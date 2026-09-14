package com.example.aistock.backend.route

import com.example.aistock.backend.dto.ErrorDto
import com.example.aistock.backend.dto.HealthDto
import com.example.aistock.backend.service.ChartService
import com.example.aistock.backend.service.UpstreamUnavailable
import com.example.aistock.backend.service.WatchlistService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

/** 标的 token 形如 sh600519 / sz300750 / hk00700。 */
private val TOKEN_PATTERN = Regex("^(sh|sz|hk)[0-9A-Za-z]+\$")

/** 装配插件与路由（main 与测试共用同一份，避免"测试装了、线上没装"的经典事故）。 */
fun Application.modules(watchlist: WatchlistService) {
    install(CallLogging) {
        level = Level.INFO
    }
    install(ContentNegotiation) {
        // encodeDefaults = true：Kotlinx 默认会**省略等于默认值的字段**，
        // 结果是 0.0 / 0L / [] 这些字段在 JSON 里直接消失（实测港股的量比就因此没了）。
        // 作为对外契约，字段必须稳定存在，客户端才不用为"字段时有时无"写一堆兜底。
        json(Json {
            prettyPrint = false
            encodeDefaults = true
        })
    }
    routing {
        apiRoutes(watchlist)
    }
}

/**
 * 路由装配。
 *
 * [chart] 给了默认实现（打真实上游），测试时注入假 client 的实例即可，
 * 不必为了可测性在 main 里多铺一层工厂。
 */
fun Route.apiRoutes(
    watchlist: WatchlistService,
    chart: ChartService = ChartService.create(),
) {

    // 探活：客户端用它来发现后端地址（多候选地址依次探测这个接口）
    get("/health") {
        call.respond(HealthDto("ok"))
    }

    // 自选行情：GET /watchlist?codes=sh600519,hk00700,...
    get("/watchlist") {
        val codes = call.request.queryParameters["codes"]
        if (codes.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorDto("invalid_codes"))
            return@get
        }
        // 入口做格式校验：不合法的 token 直接挡掉，别让它流到上游
        if (!codes.split(",").all { it.matches(TOKEN_PATTERN) }) {
            call.respond(HttpStatusCode.BadRequest, ErrorDto("invalid_codes"))
            return@get
        }
        try {
            call.respond(watchlist.fetchWatchlist(codes))
        } catch (e: UpstreamUnavailable) {
            // 上游挂了是 502（网关错误），不是 500（我们自己的 bug）——这个区分对排障很重要
            call.respond(HttpStatusCode.BadGateway, ErrorDto("upstream_unavailable"))
        } catch (e: Throwable) {
            call.respond(HttpStatusCode.InternalServerError, ErrorDto("internal_error"))
        }
    }

    // 单只股票分析：GET /analysis/{token}
    get("/analysis/{token}") {
        val token = call.parameters["token"]
        if (token == null || !token.matches(TOKEN_PATTERN)) {
            call.respond(HttpStatusCode.BadRequest, ErrorDto("invalid_token"))
            return@get
        }
        try {
            val dto = watchlist.fetchAnalysis(token)
            if (dto == null) {
                call.respond(HttpStatusCode.NotFound, ErrorDto("not_found"))
            } else {
                call.respond(dto)
            }
        } catch (e: UpstreamUnavailable) {
            call.respond(HttpStatusCode.BadGateway, ErrorDto("upstream_unavailable"))
        } catch (e: Throwable) {
            call.respond(HttpStatusCode.InternalServerError, ErrorDto("internal_error"))
        }
    }

    // 走势：GET /chart?token=sh600519&period=minute|m60|day|week|month
    get("/chart") {
        val token = call.request.queryParameters["token"]
        if (token == null || !token.matches(TOKEN_PATTERN)) {
            call.respond(HttpStatusCode.BadRequest, ErrorDto("invalid_token"))
            return@get
        }
        // period 不在支持集里明确报错，而不是悄悄按分时返回 ——
        // 让调用方以为拿到的是日 K 是最坏的情况：图上会有线，但它是错的。
        val period = call.request.queryParameters["period"] ?: ChartService.PERIOD_MINUTE
        try {
            when (period) {
                ChartService.PERIOD_MINUTE -> {
                    val dto = chart.fetchMinute(token)
                    if (dto == null) call.respond(HttpStatusCode.NotFound, ErrorDto("chart_not_found"))
                    else call.respond(dto)
                }
                in ChartService.SUPPORTED_KLINE_PERIODS -> {
                    val dto = chart.fetchKline(token, period)
                    if (dto == null) call.respond(HttpStatusCode.NotFound, ErrorDto("chart_not_found"))
                    else call.respond(dto)
                }
                else -> call.respond(HttpStatusCode.BadRequest, ErrorDto("unsupported_period"))
            }
        } catch (e: UpstreamUnavailable) {
            call.respond(HttpStatusCode.BadGateway, ErrorDto("upstream_unavailable"))
        } catch (e: Throwable) {
            // 吞异常再回 500 等于把 bug 藏起来——必须留日志，排障时才有的看
            call.application.environment.log.error("GET /chart internal error", e)
            call.respond(HttpStatusCode.InternalServerError, ErrorDto("internal_error"))
        }
    }
}
