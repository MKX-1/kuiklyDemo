package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppShape
import com.example.aistock.theme.AppSpace
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 页面三态：加载 / 失败可重试 / 空。
 *
 * 为什么要有这一层：这三个状态每个页面都会遇到，各自手写一遍的结果必然是
 * 「三个页面三种加载样式」。抽成组件后全 App 长相统一，用户不用重新学习。
 *
 * 这里只放**呈现**，不放逻辑：当前该显示哪个态，由页面根据数据状态决定。
 */

/** 加载态：三个错相呼吸的点。错相位让它像「扫描」，而不是三颗静止的灯。 */
@Composable
fun LoadingBox(
    text: String = "正在拉取行情…",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulseDot(color = AppColors.Accent, phaseMs = 0)
            Spacer(modifier = Modifier.width(6.dp))
            PulseDot(color = AppColors.Accent, phaseMs = 150)
            Spacer(modifier = Modifier.width(6.dp))
            PulseDot(color = AppColors.Accent, phaseMs = 300)
        }
        Spacer(modifier = Modifier.height(AppSpace.Sm))
        Text(text = text, color = AppColors.TextWeak, fontSize = AppText.Caption)
    }
}

/**
 * 失败态：说明 + **可重试按钮**。
 *
 * 「失败可重试」不是锦上添花：拉不到数据时若只显示一行错误文字，
 * 用户唯一的出路是杀进程重开。给一个明确的重试入口，这条路径才算做完。
 */
@Composable
fun ErrorBox(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryText: String = "重新加载",
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "数据没能取回来",
            color = AppColors.TextMain,
            fontSize = AppText.Body,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(AppSpace.Xs))
        Text(text = message, color = AppColors.TextWeak, fontSize = AppText.Caption)
        Spacer(modifier = Modifier.height(AppSpace.Lg))
        Text(
            text = retryText,
            color = AppColors.Accent,
            fontSize = AppText.Caption,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(color = AppColors.AccentSoft, shape = AppShape.Pill)
                .border(width = 1.dp, color = AppColors.Accent.copy(alpha = 0.5f), shape = AppShape.Pill)
                .clickable { onRetry() }
                .padding(horizontal = 16.dp, vertical = 7.dp),
        )
    }
}

/**
 * 空态：合法但没有内容（例如筛选后没有命中）。
 *
 * 与失败态**刻意区分**：失败态的出路是「重试」，空态的出路是「换个筛选条件」，
 * 给错按钮会让用户白点几次。
 */
@Composable
fun EmptyBox(
    message: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, color = AppColors.TextSub, fontSize = AppText.Body)
        if (hint != null) {
            Spacer(modifier = Modifier.height(AppSpace.Xs))
            Text(text = hint, color = AppColors.TextWeak, fontSize = AppText.Caption)
        }
    }
}
