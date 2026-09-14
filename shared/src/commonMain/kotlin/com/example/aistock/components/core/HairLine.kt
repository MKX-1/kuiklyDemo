package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppShape
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 1px 分隔线。
 *
 * 深色界面上分隔线**不能亮**：一条 #E5E7EB 级别的线放到近黑底上会变成刺眼的横杠。
 * 这里用 #16202F（比面板底 #0C1220 只亮一点点），刚好够「感觉到有分界」。
 *
 * @param accent 左侧加一小段青色刻度。用于「数据区与说明区」的分界，
 *               比纯线更有仪表感；纯列表分隔就用默认的整条淡线。
 */
@Composable
fun HairLine(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    if (!accent) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color = AppColors.LineSoft),
        )
        return
    }

    Row(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(1.dp)
                .background(color = AppColors.Accent.copy(alpha = 0.5f), shape = AppShape.Handle),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(color = AppColors.LineSoft),
        )
    }
}
