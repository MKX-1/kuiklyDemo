package com.example.aistock.data

/**
 * AI 分析「叙事化」纯函数集合 —— 本地派生，**只改表述方式，不改推导逻辑**
 * （推导在 RuleEngine / 后端因子引擎；接真 LLM 后这些函数可由 LLM 产出，此处保留为兜底文案）。
 *
 * 设计原则（从参考架构提炼）：
 *  - 每段结尾必须带句号：段落会被首尾相接展示，缺标点会粘成一坨；
 *  - 措施词必须与操作建议一致（「建议回避」却写「可分批建仓」是绝对矛盾）；
 *  - 口径只说「今日」：信号原型来自当日行情，写成「近 5 日」是无依据的拔高。
 */
object AiNarrative {

    /**
     * 趋势叙事：涨跌幅 + 信号 + 趋势标签连成一句人话。
     */
    fun trend(item: StockItem, analysis: AiAnalysis): String {
        val direction = when {
            item.changePct > 0 -> "涨 ${formatPctSigned(item.changePct)}"
            item.changePct < 0 -> "跌 ${formatPctSigned(-item.changePct)}"
            else -> "平盘"
        }
        val signal = if (item.aiProfile.signal.isBlank()) "量价信号暂不明确" else "量价表现为${item.aiProfile.signal}"
        return "${item.name}今日$direction，$signal；${analysis.trendLabel}。"
    }

    /**
     * 风险叙事：振幅波动 / 估值 / 趋势方向三要素 + 评级。
     */
    fun risk(item: StockItem, analysis: AiAnalysis): String {
        val volatility = if (item.amplitude >= 3.0) {
            "振幅 ${formatDouble2(item.amplitude)}% 波动较大"
        } else {
            "振幅 ${formatDouble2(item.amplitude)}% 波动可控"
        }
        val valuation = when {
            item.pe <= 0.0 -> "市盈率为负，注意基本面"
            item.pe <= 20.0 -> "PE ${formatDouble2(item.pe)} 估值偏低"
            item.pe <= 40.0 -> "PE ${formatDouble2(item.pe)} 估值适中"
            else -> "PE ${formatDouble2(item.pe)} 估值偏高"
        }
        val direction = if (item.changePct >= 0) "趋势向上" else "短期承压"
        return "$volatility · $valuation · $direction。综合风险评级：${analysis.riskLevel}。"
    }

    /**
     * 操作叙事：建议 + 评分 + 目标/止损，连成一句可执行的话。
     */
    fun action(item: StockItem, analysis: AiAnalysis): String {
        val act = item.aiProfile.action
        val target = formatFen(analysis.targetPrice)
        val stop = formatFen(analysis.stopLossPrice)
        val plan = when {
            act.contains("回避") -> "暂不建仓，等趋势明朗再评估"
            act.contains("观望") -> "目标价 $target，跌破 $stop 再评估"
            else -> "可分批建仓，目标价位 $target，止损位 $stop"
        }
        return "综合建议：$act（推荐指数 ${analysis.score} 分）。$plan。"
    }

    /** 分析阶段轮播词（弹层思考态）：与真实取数阶段大致对应。 */
    val THINKING_WORDS = listOf("正在拉取行情…", "计算因子…", "生成结论…")
}
