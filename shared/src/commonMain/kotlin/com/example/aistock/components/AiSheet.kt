package com.example.aistock.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.example.aistock.components.core.FactorBar
import com.example.aistock.components.core.HairLine
import com.example.aistock.data.AiAnalysis
import com.example.aistock.data.AiNarrative
import com.example.aistock.data.StockItem
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppFont
import com.example.aistock.theme.AppShape
import com.example.aistock.theme.AppSpace
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.alpha
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * AI 分析抽屉（长按股票卡片拉起）—— 纸墨风的「AI 签纸」。
 *
 * 节奏（借鉴参考实现的编排思想）：
 *   弹出 → 分析阶段（思考词轮播，时长由**真实取数**决定，不写死延时）
 *        → 结论就绪后单一 progress 驱动三段按时间窗错峰舒展
 *          （趋势 [0, 0.34] / 风险 [0.34, 0.67] / 操作 [0.67, 1]）
 *        → 「查看完整报告」随操作段一起出现。
 *
 * 弹层本体是页面内 overlay（半透明遮罩 + 底部纸面板上滑），不依赖 ModalBottomSheet
 * —— 本 fork 未验证该组件，overlay + animateFloatAsState 是已验证能力。
 */
@Composable
fun AiSheet(
    item: StockItem,
    analysis: AiAnalysis?,
    analyzing: Boolean,
    onDismiss: () -> Unit,
    onOpenReport: (String) -> Unit,
) {
    // 结论舒展进度：analysis 就绪后从 0 → 1（REVEAL_MS 总时长，三段按窗口取值）
    var revealStarted by remember(item.id) { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (revealStarted && !analyzing && analysis != null) 1f else 0f,
        animationSpec = tween(durationMillis = REVEAL_MS),
    )

    LaunchedEffect(item.id, analysis) {
        if (analysis != null && !analyzing) revealStarted = true
    }

    // 思考词轮播：每 900ms 前进一词（analyzing 期间）
    var wordIndex by remember(item.id) { mutableStateOf(0) }
    LaunchedEffect(item.id, analyzing) {
        while (analyzing) {
            wordIndex = (wordIndex + 1) % AiNarrative.THINKING_WORDS.size
            kotlinx.coroutines.delay(900)
        }
    }

    // ---- 面板 ----
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Panel, AppShape.SheetTop)
            .padding(horizontal = AppSpace.ScreenEdge)
            .padding(top = 10.dp, bottom = AppSpace.Lg),
    ) {
        // 把手
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .background(AppColors.Line, AppShape.Handle)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 标题行：AI 印章方块 + 衬线标题 + 评分
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "AI",
                color = AppColors.Panel,
                fontSize = AppText.Tiny,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(AppColors.Accent, AppShape.Badge)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI 分析 · ${item.name}",
                color = AppColors.TextMain,
                fontSize = AppText.Title,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppFont.Serif,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (analysis != null && !analyzing) {
                Text(
                    text = "${analysis.score} 分",
                    color = AppColors.Accent,
                    fontSize = AppText.Title,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFont.Serif,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        HairLine()
        Spacer(modifier = Modifier.height(AppSpace.Md))

        when {
            // ---- 分析阶段：思考词轮播 ----
            analyzing || analysis == null -> {
                Column(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    ThinkingWord(word = AiNarrative.THINKING_WORDS[wordIndex])
                }
            }

            // ---- 结论就绪：三段错峰 ----
            else -> {
                // 因子分解（三因子是评级的依据，先给依据再给结论）
                FactorBar(label = "动量", value = analysis.factors.momentum, max = 60)
                Spacer(modifier = Modifier.height(AppSpace.Sm))
                FactorBar(label = "价值", value = analysis.factors.value, max = 25)
                Spacer(modifier = Modifier.height(AppSpace.Sm))
                FactorBar(label = "风险", value = analysis.factors.risk, max = 8, color = AppColors.Warning)
                Spacer(modifier = Modifier.height(AppSpace.Md))

                // 三段叙事：趋势 / 风险 / 操作
                SheetSection(
                    title = "趋势",
                    text = AiNarrative.trend(item, analysis),
                    progress = seg(progress, 0f),
                )
                SheetSection(
                    title = "风险",
                    text = AiNarrative.risk(item, analysis),
                    progress = seg(progress, 0.34f),
                )
                SheetSection(
                    title = "操作",
                    text = AiNarrative.action(item, analysis),
                    progress = seg(progress, 0.67f),
                )

                // 查看完整报告（随操作段出现）
                val reportAlpha = seg(progress, 0.67f)
                if (reportAlpha > 0.5f) {
                    Spacer(modifier = Modifier.height(AppSpace.Sm))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.AccentSoft, AppShape.Card)
                            .clickable { onOpenReport(item.code) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .alpha(reportAlpha),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "查看完整报告",
                            color = AppColors.Accent,
                            fontSize = AppText.Body,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "→", color = AppColors.Accent, fontSize = AppText.Body)
                    }
                }

                Spacer(modifier = Modifier.height(AppSpace.Sm))
                Text(
                    text = if (analysis.source == "llm") "本结论由大模型基于当日行情生成，仅供参考，不构成投资建议"
                           else "AI 结论由规则引擎生成，仅供参考，不构成投资建议",
                    color = AppColors.TextWeak,
                    fontSize = AppText.Micro,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        // 点遮罩关闭的替代：底部也给一个明确的「收起」热区
        Text(
            text = "收起",
            color = AppColors.TextWeak,
            fontSize = AppText.Small,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onDismiss() }
                .padding(horizontal = 20.dp, vertical = 6.dp),
        )
    }
}

/** 段落窗口插值：progress 越过 [start] 后在 0.33 宽度内从 0 到 1。 */
private fun seg(progress: Float, start: Float): Float =
    ((progress - start) / 0.33f).coerceIn(0f, 1f)

private const val REVEAL_MS = 1000

/** 一段结论：小标题（印章 + 衬线）+ 叙事文字，随 [alpha] 错峰入场。 */
@Composable
private fun SheetSection(title: String, text: String, progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppSpace.Md)
            .alpha(progress),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .background(AppColors.Accent),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = AppColors.TextMain,
                fontSize = AppText.Body,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppFont.Serif,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = AppColors.TextSub,
            fontSize = AppText.Caption,
        )
    }
}

/** 思考态：墨点 + 轮播词。 */
@Composable
private fun ThinkingWord(word: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .background(AppColors.Accent, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = word, color = AppColors.TextSub, fontSize = AppText.Body)
    }
}
