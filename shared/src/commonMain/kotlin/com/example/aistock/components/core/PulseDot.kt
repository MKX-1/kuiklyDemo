package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.aistock.theme.AppMotion
import com.example.aistock.theme.AppShape
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.animation.core.FastOutSlowInEasing
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.alpha
import com.tencent.kuikly.compose.ui.draw.scale
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 呼吸圆点：让「实时」这类状态**看起来是活的**。
 *
 * 静态的绿点容易被读成「一个装饰」；有节律地缩放 + 明暗呼吸之后，
 * 用户会下意识认为「数据在持续更新」—— 这正是状态指示器该传达的信息。
 *
 * 实现方式是「翻转状态 + 单程 tween 循环」，而不是无限动画组件：
 * 本项目实测可用的动画 API 只有 `animateFloatAsState` + `tween` 这一套，
 * 循环靠 `LaunchedEffect` 里翻转状态触发目标值变化来驱动，稳定且不引额外依赖。
 *
 * @param phaseMs 相位偏移（毫秒）。一排点分别给 0 / 150 / 300，
 *                就会形成「扫描」式的波浪，而不是三颗同时明灭的灯。
 */
@Composable
fun PulseDot(
    color: Color,
    modifier: Modifier = Modifier,
    dot: Dp = 7.dp,
    phaseMs: Int = 0,
    minScale: Float = 0.72f,
    minAlpha: Float = 0.45f,
) {
    var on by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (phaseMs > 0) delay(phaseMs.toLong())
        while (true) {
            on = !on
            delay(AppMotion.PULSE_MS.toLong())
        }
    }

    val v by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = tween(durationMillis = AppMotion.PULSE_MS, easing = FastOutSlowInEasing),
    )

    Box(
        modifier = modifier
            .size(dot)
            .scale(minScale + (1f - minScale) * v)
            .alpha(minAlpha + (1f - minAlpha) * v)
            .background(color = color, shape = AppShape.Pill),
    )
}

/**
 * 状态角标：小圆点 + 文案，外面套一层同色描边。
 *
 * 三态（实时 / 非实时 / 示例数据）**必须**用同一个组件、出现在同一个位置 ——
 * 让用户养成「看这里就知道数据可不可信」的习惯。
 * 把兜底数据伪装成实时，是这个产品里最不能犯的错误。
 *
 * @param pulsing 只有「实时」让它呼吸；缓存和离线是静止的，这个对比本身就是信息
 */
@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
) {
    Row(
        modifier = modifier
            .background(color = color.copy(alpha = 0.12f), shape = AppShape.Pill)
            .border(width = 1.dp, color = color.copy(alpha = 0.32f), shape = AppShape.Pill)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pulsing) {
            PulseDot(color = color, dot = 6.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color = color.copy(alpha = 0.75f), shape = AppShape.Pill),
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            color = color,
            fontSize = AppText.Micro,
            fontWeight = FontWeight.Medium,
        )
    }
}
