package com.example.aistock.data

/**
 * 规则引擎：把行情字段机械地推导成「AI 画像 / 分析结论 / 动态标签」。
 *
 * ⚠️ 定位必须说清楚：这是**演示用的规则引擎，不是真正的大模型分析**。
 * 它的价值在于把「AI 结论 -> 数据结构 -> UI 展示」这条链路完整跑通；
 * 接真 LLM 时，只要替换本文件的实现（或在后端换成 LLM provider），UI 一行都不用改。
 *
 * 所有函数都是纯函数（输入相同输出相同，无副作用），好测试、好推理。
 */

/** 标签阈值：与后端逐条镜像，两边必须一致。 */
object FactorThresh {
    const val TAG_VOLUME_RATIO = 1.5
    const val TAG_AMPLITUDE_PCT = 4.0
    const val TAG_LEAD_PCT = 3.0
    const val STALE_MS = 5 * 60_000L
}

/** 由当日涨跌幅 + 市盈率推导 AI 四维画像。 */
fun deriveAiProfile(item: StockItem): AiProfile {
    val changePct = item.changePct
    val pe = item.pe

    val action = when {
        changePct > AiThresh.ACTION_FOCUS_PCT -> AiLabels.ACTION_FOCUS
        changePct in AiThresh.ACTION_DIP_PCT..AiThresh.ACTION_FOCUS_PCT -> AiLabels.ACTION_DIP
        changePct > AiThresh.ACTION_AVOID_PCT -> AiLabels.ACTION_HOLD
        else -> AiLabels.ACTION_AVOID
    }

    val signal = when {
        changePct > AiThresh.SIGNAL_SURGE_PCT && pe <= 0 -> AiLabels.SIGNAL_VOLUME
        changePct > AiThresh.SIGNAL_SURGE_PCT -> AiLabels.SIGNAL_MACD
        pe in 1.0..AiThresh.PE_GOOD -> AiLabels.SIGNAL_BOTTOM
        else -> AiLabels.SIGNAL_OVERSOLD
    }

    // 评分：以 50 分为基准，动量 / 价值 / 风险三部分增减
    val (momentum, value, risk) = scoreParts(changePct, pe)
    val score = (50 + momentum + value + risk).coerceIn(0, 100)

    val scenario = when {
        action == AiLabels.ACTION_FOCUS && score >= AiThresh.SCORE_HIGH -> AiLabels.SCENARIO_ADD
        action == AiLabels.ACTION_DIP -> AiLabels.SCENARIO_BUILD
        action == AiLabels.ACTION_HOLD -> AiLabels.SCENARIO_KEEP
        else -> AiLabels.SCENARIO_CUT
    }
    return AiProfile(action, signal, score, scenario)
}

/** 评分三分项，量程各不相同（0-60 / 0-25 / 0-8），展示时必须标出量程，否则会被误读。 */
private fun scoreParts(changePct: Double, pe: Double): Triple<Int, Int, Int> = Triple(
    (changePct.coerceIn(-5.0, 8.0) / 8.0 * 60.0).toInt(),
    if (pe in 1.0..AiThresh.PE_GOOD) 25 else if (pe > AiThresh.PE_BAD) 10 else 15,
    when {
        changePct < -2.0 -> 0
        changePct < 0.0 -> 5
        else -> 8
    },
)

/** 由画像 + 行情推导分析结论（趋势 / 风险 / 目标价止损价 / 因子明细）。 */
fun deriveAiAnalysis(item: StockItem): AiAnalysis {
    val p = item.aiProfile
    val pct = item.changePct
    val (momentum, value, risk) = scoreParts(pct, item.pe)

    val trendLabel = when {
        pct > AiThresh.ACTION_FOCUS_PCT -> "短期看涨信号明显"
        pct > 0.0 -> "短期震荡偏强"
        pct > -2.0 -> "短期窄幅整理"
        else -> "短期承压回落"
    }
    val riskLevel = when {
        p.score >= AiThresh.SCORE_HIGH -> "低风险"
        p.score >= AiThresh.SCORE_MID -> "中低风险"
        else -> "中高风险"
    }
    return AiAnalysis(
        trendLabel = trendLabel,
        trendText = "驱动${p.action}（${p.score}分），${p.signal}；今日涨跌幅 ${formatPct(pct)}。",
        riskLevel = riskLevel,
        riskText = "当前评级：$riskLevel",
        score = p.score,
        targetPrice = (item.price + item.price * AiThresh.TARGET_RATIO).toLong(),
        stopLossPrice = (item.price - item.price * AiThresh.STOP_RATIO).toLong(),
        factors = AiFactors(momentum, value, risk, item.industry, benchmarkDelta = item.benchmarkDelta),
    )
}

/**
 * 振幅：`(最高 − 最低) / 昨收`。
 *
 * 腾讯接口部分市场不给振幅字段，缺了会显示成「振幅 0.00% 波动可控」——
 * 0 振幅被当成「波动可控」是**假信息**，比缺信息更糟。三个输入其实都有，昨收 = 现价 − 涨跌额。
 */
fun deriveAmplitude(item: StockItem): Double {
    if (item.amplitude > 0.0) return item.amplitude
    val prevClose = item.price - item.change
    if (prevClose <= 0L || item.high <= 0L || item.low <= 0L) return item.amplitude
    return (item.high - item.low).toDouble() / prevClose * 100.0
}

/** 个股涨跌幅 − 对应指数涨跌幅。指数缺失时返回 null（UI 显示「—」，绝不臆造）。 */
fun deriveBenchmarkDelta(item: StockItem, indexChangePct: Double?): Double? =
    indexChangePct?.let { item.changePct - it }

/**
 * 动态标签。规则与后端逐条镜像；0 值字段自动不触发（数据缺失时不会假触发）。
 */
fun deriveTags(item: StockItem): List<String> {
    val tags = mutableListOf<String>()
    item.benchmarkDelta?.let { if (it > 0) tags += "强于大盘" }
    if (item.volumeRatio >= FactorThresh.TAG_VOLUME_RATIO) {
        tags += "量比异动"
        if (item.changePct > 0) tags += "放量上攻"
    }
    if (item.amplitude >= FactorThresh.TAG_AMPLITUDE_PCT) tags += "振幅放大"
    if (item.changePct >= FactorThresh.TAG_LEAD_PCT) tags += "领涨"
    if (item.pe in 1.0..AiThresh.PE_GOOD && item.changePct < 0) tags += "低位企稳"
    return tags
}

/**
 * 补全派生字段：振幅 → 基准差 → 标签。
 *
 * 顺序有依赖：振幅要先算出来，「振幅放大」标签才可能命中。
 * 每个字段都遵循「已有值就保留」——后端（将来含 LLM）产出的值不会被本地规则悄悄顶掉。
 */
fun enrich(item: StockItem, indexChangePct: Double?): StockItem {
    val withAmplitude = item.copy(amplitude = deriveAmplitude(item))
    val withDelta = if (withAmplitude.benchmarkDelta == null) {
        withAmplitude.copy(benchmarkDelta = deriveBenchmarkDelta(withAmplitude, indexChangePct))
    } else {
        withAmplitude
    }
    return withDelta.copy(tags = withDelta.tags.ifEmpty { deriveTags(withDelta) })
}

/** 列表级摘要：涨跌家数 + 最强最弱 + 一句倾向。 */
fun deriveSummary(stocks: List<StockItem>, generatedAt: Long): AiSummary {
    if (stocks.isEmpty()) return AiSummary("暂无行情数据，下拉刷新重试", generatedAt)
    val up = stocks.count { it.changePct > 0 }
    val down = stocks.count { it.changePct < 0 }
    val best = stocks.maxByOrNull { it.changePct }
    val worst = stocks.minByOrNull { it.changePct }
    val avg = stocks.map { it.changePct }.average()
    val tone = if (avg >= 0) "今日整体偏强，关注领涨股" else "今日偏弱，注意控制仓位"
    val text = "自选 ${up} 涨 ${down} 跌；最强「${best?.name}」${formatPctSigned(best?.changePct ?: 0.0)}，" +
        "最弱「${worst?.name}」${formatPctSigned(worst?.changePct ?: 0.0)}。$tone。"
    return AiSummary(text, generatedAt)
}

/**
 * 按市场前缀取对应大盘指数 token。
 * 指数行情是**真实拉取**的，不用任何硬编码数值——
 * 编一组指数涨跌幅塞进界面，比不显示这个字段更糟。
 */
object BenchmarkIndex {
    fun indexTokenFor(marketOfCode: String): String = when (marketOfCode) {
        "sh" -> "sh000001"   // 上证指数
        "sz" -> "sz399001"   // 深证成指
        else -> "hkHSI"      // 恒生指数
    }

    /** 由股票代码推断市场前缀。 */
    fun marketOf(code: String): String = when {
        code.length == 5 -> "hk"
        code.startsWith("6") || code.startsWith("9") -> "sh"
        else -> "sz"
    }
}
