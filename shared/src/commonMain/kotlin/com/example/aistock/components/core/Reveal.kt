package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.aistock.theme.AppMotion
import com.tencent.kuikly.compose.animation.core.FastOutSlowInEasing
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.alpha
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 入场动效原语：内容首次出现时，从「透明 + 偏移」过渡到原位。
 *
 * 用法：给同屏的一批元素依次传入 index，它们就会**错峰涌现**，
 * 而不是「啪一下全蹦出来」——这是「编排好的入场序列」区别于「零散微交互」的关键。
 *
 * ```kotlin
 * stocks.forEachIndexed { i, s -> Reveal(i) { StockCard(s) } }
 * ```
 *
 * 两个必须知道的实现细节（都是踩过的坑）：
 *
 * 1. **动画值只能走「值参数」（alpha / offset），不能在 `graphicsLayer{}` 的 lambda 里读**。
 *    后者在本框架下实测会「假死」——值停在首帧不再更新；走值参数则每帧都跟着重组走。
 * 2. **只动透明度和位移，不动尺寸**。入场阶段改尺寸会让相邻元素跟着抖动，观感很廉价。
 *
 * 另外：在 LazyColumn 里，条目滚出可视区被回收、再滚回来会**重新入场一次**。
 * 短列表观感正常（也算一种「滚动显现」）；长列表若嫌吵，就别逐项包，改成整块包一层。
 *
 * @param index 同屏第几项，决定错峰延迟（自动封顶，见 [AppMotion.STAGGER_MAX_MS]）
 * @param riseDp 初始下沉距离，0 表示不位移只淡入
 * @param slideFromLeft 改成从左侧滑入（用于「数据条横向展开」的仪表盘感）
 */
@Composable
fun Reveal(
    index: Int = 0,
    modifier: Modifier = Modifier,
    riseDp: Dp = 10.dp,
    slideFromLeft: Boolean = false,
    content: @Composable () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val delayMs = (index * AppMotion.STAGGER_STEP_MS).coerceAtMost(AppMotion.STAGGER_MAX_MS)
        if (delayMs > 0) delay(delayMs.toLong())
        shown = true
    }

    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(
            durationMillis = AppMotion.APPEAR_MS,
            easing = FastOutSlowInEasing,
        ),
    )
    val remain = 1f - progress

    Box(
        modifier = modifier
            .alpha(progress)
            .offset(
                x = if (slideFromLeft) -riseDp * remain else 0.dp,
                y = if (slideFromLeft) 0.dp else riseDp * remain,
            ),
    ) {
        content()
    }
}
