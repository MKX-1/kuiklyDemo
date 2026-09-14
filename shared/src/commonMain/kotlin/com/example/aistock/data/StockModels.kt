package com.example.aistock.data

/**
 * 领域模型层：与「数据从哪来」完全无关的纯数据结构。
 *
 * 关键设计：所有金额统一用「分」存 Long，不用浮点。
 * 原因：浮点数存钱会有精度误差（0.1+0.2 != 0.3），金融数据必须用整数最小单位。
 * 展示时再除以 100 转成「元」。
 */
data class StockItem(
    val id: String,
    val code: String,          // 纯代码，如 600519
    val name: String,
    val price: Long,           // 现价（分）
    val change: Long,          // 涨跌额（分，带符号）
    val changePct: Double,     // 涨跌幅（%）
    val open: Long,            // 今开（分）
    val high: Long,            // 今日最高（分）
    val low: Long,             // 今日最低（分）
    val marketCap: Long,       // 总市值（元）
    val floatCap: Long,        // 流通市值（元）
    val pe: Double,            // 市盈率
    val turnover: Double = 0.0,      // 换手率 %
    val volumeRatio: Double = 0.0,   // 量比（倍数）
    val amplitude: Double = 0.0,     // 振幅 %
    val amount: Long = 0L,           // 成交额（元）
    val industry: String = "未分类",
    val benchmarkDelta: Double? = null,  // 个股涨跌幅 − 大盘指数涨跌幅；null = 指数缺失
    val tags: List<String> = emptyList(),
    val aiProfile: AiProfile = AiProfile(AiLabels.ACTION_HOLD, AiLabels.SIGNAL_BOTTOM, 50, AiLabels.SCENARIO_KEEP),
)

/** AI 四维画像：操作建议 / 依据信号 / 评分 / 场景。 */
data class AiProfile(
    val action: String,
    val signal: String,
    val score: Int,      // 0-100
    val scenario: String,
)

/**
 * 画像取值的单一事实源。
 * 散着写字符串，改一处漏三处，分组时拼错一个值就会掉进「未命中」桶。
 */
object AiLabels {
    const val ACTION_FOCUS = "重点关注"
    const val ACTION_DIP = "低吸关注"
    const val ACTION_HOLD = "持股观望"
    const val ACTION_AVOID = "建议回避"

    const val SIGNAL_VOLUME = "量能放大"
    const val SIGNAL_MACD = "MACD金叉"
    const val SIGNAL_BOTTOM = "低位企稳"
    const val SIGNAL_OVERSOLD = "超跌反弹"

    const val SCENARIO_ADD = "建议加自选"
    const val SCENARIO_BUILD = "建议建仓"
    const val SCENARIO_KEEP = "继续持有"
    const val SCENARIO_CUT = "建议减仓"
}

/** 规则引擎的阈值：单一事实源，客户端与后端保持逐条一致。 */
object AiThresh {
    const val SCORE_HIGH = 85
    const val SCORE_MID = 70
    const val AI_ENABLED = 80         // 分数达到这个值才在卡片上展示 AI 建议行
    const val ACTION_FOCUS_PCT = 3.0
    const val ACTION_DIP_PCT = 1.0
    const val ACTION_AVOID_PCT = -1.0
    const val SIGNAL_SURGE_PCT = 2.0
    const val PE_GOOD = 20.0
    const val PE_BAD = 40.0
    const val TARGET_RATIO = 0.08     // 目标价 = 现价 ×(1+8%)
    const val STOP_RATIO = 0.05       // 止损价 = 现价 ×(1-5%)
}

/**
 * 数据来源三态。
 * UI 必须如实告诉用户当前看到的是实时行情、缓存还是示例数据——
 * 把示例数据伪装成实时行情，是这个领域里最严重的错误。
 */
enum class DataSource { LIVE, CACHE, OFFLINE }

data class AiSummary(
    val text: String,
    val generatedAt: Long,
    val stale: Boolean = false,
)

data class AiFactors(
    val momentum: Int,      // 动量分 0-60
    val value: Int,         // 价值分 0-25
    val risk: Int,          // 风险分 0-8
    val industry: String,
    val industryRank: Int = -1,
    val benchmarkDelta: Double? = null,
)

data class AiAnalysis(
    val trendLabel: String,
    val trendText: String,
    val riskLevel: String,
    val riskText: String,
    val score: Int,
    val targetPrice: Long,     // 分
    val stopLossPrice: Long,   // 分
    val factors: AiFactors,
)

/**
 * 一次行情拉取的完整结果：数据 + 元信息。
 *
 * 为什么要把「来源」和「时间」跟数据捆在一起返回：页面必须能显示
 * 「行情非实时（更新于 10:32:11）」这类提示，如果只返回 List<StockItem>，
 * 页面就无从判断自己拿到的是实时还是兜底数据。
 */
data class WatchlistBundle(
    val stocks: List<StockItem>,
    val fetchedAt: Long,
    val source: DataSource,
    val summary: AiSummary = AiSummary("", 0L),
    val missing: Int = 0,     // 本次没取到的条目数（部分失败要在 UI 上说出来）
)

/**
 * 数据源接口：页面只依赖这个接口，不关心背后是后端、腾讯直连还是本地样例。
 * 这就是「面向接口编程」——将来换成别的数据源，页面代码一行都不用改。
 */
interface StockApi {
    suspend fun fetchWatchlist(): WatchlistBundle
    suspend fun fetchAiAnalysis(code: String): AiAnalysis
    suspend fun fetchStock(code: String): StockItem?
}
