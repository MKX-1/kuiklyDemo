package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppShape
import com.example.aistock.theme.AppSpace
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 分节标题：一道**短青刻度** + 标题文字 +（可选）右侧说明。
 *
 * 那个 3×12dp 的小竖条是全 App 的「仪表盘签名」——面板、分节、弹层都用它开头，
 * 反复出现后用户会形成肌肉记忆：看到青条就知道「这是一节内容的开始」。
 *
 * 这个刻度是**用 Box 画的**，不是 Canvas：它尺寸固定，一个带圆角的方块就够，
 * 没必要为它引一层绘制逻辑。
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    accent: Boolean = true,
) {
    Row(
        modifier = modifier.padding(vertical = AppSpace.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accent) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 12.dp)
                    .background(color = AppColors.Accent, shape = AppShape.Handle),
            )
            Spacer(modifier = Modifier.width(AppSpace.Sm))
        }
        Text(
            text = title,
            color = AppColors.TextMain,
            fontSize = AppText.Title,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(
                text = trailing,
                color = AppColors.TextWeak,
                fontSize = AppText.Caption,
            )
        }
    }
}
