package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppShape
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 通用小徽章：文字在色块内**水平垂直都居中**。
 *
 * 为什么需要它：之前各处徽章都是「Text + background + vertical padding」的写法，
 * 中文字形的行高会让文字在色块里明显偏上（用户实测反馈「实时/AI/强于大盘」全部如此）。
 * 统一收敛到这里：固定高度 + [Alignment.Center]，一处修好，处处生效。
 *
 * @param container 背景色（实心墨块）；传 Color.Transparent 则只有文字
 * @param bordered 描边式（TagChip 的空心样式），描边色跟随 [content]
 * @param height 固定高度（垂直居中的基准）
 */
@Composable
fun Badge(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    bordered: Boolean = false,
    height: Dp = 18.dp,
    horizontalPadding: Dp = 6.dp,
    fontSize: com.tencent.kuikly.compose.ui.unit.TextUnit = AppText.Tiny,
    fontWeight: FontWeight = FontWeight.Medium,
) {
    val base = if (bordered) {
        Modifier.border(1.dp, content, AppShape.Badge)
    } else {
        Modifier.background(container, AppShape.Badge)
    }
    Box(
        modifier = modifier
            .then(base)
            .padding(horizontal = horizontalPadding)
            .height(height),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = content,
            fontSize = fontSize,
            fontWeight = fontWeight,
        )
    }
}

/** 常用预设：AI 印章（黛青底、纸色字）。 */
@Composable
fun AiBadge(modifier: Modifier = Modifier) {
    Badge(
        text = "AI",
        container = AppColors.Accent,
        content = AppColors.Panel,
        modifier = modifier,
        height = 18.dp,
        horizontalPadding = 5.dp,
        fontWeight = FontWeight.Bold,
    )
}
