package com.example.aistock.components.core

import androidx.compose.runtime.Composable
import com.example.aistock.theme.AppColors
import com.example.aistock.theme.AppFont
import com.example.aistock.theme.AppSpace
import com.example.aistock.theme.AppText
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 分节标题 —— 报刊的「栏首」。
 *
 * 三个排印装置（文气的来源，重复出现形成全 App 的签名）：
 *  1. **印章方块**：6dp 黛青小方块，像盖章落款，替代上一版的「青刻度」；
 *  2. **衬线标题**：宋体 SemiBold，报刊栏目标题的仪式感；
 *  3. **双细线**：标题下方一粗一细两条横线——这是报纸版面最经典的
 *     分隔装置（英语报纸叫 double rule），比任何圆角卡片都更有「出版物」感。
 *
 * @param accent false 时不画印章方块（用于次要分节，弱化层级）
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    accent: Boolean = true,
) {
    Column(modifier = modifier.padding(top = AppSpace.Sm, bottom = AppSpace.Sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (accent) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color = AppColors.Accent),
                )
                Spacer(modifier = Modifier.width(AppSpace.Sm))
            }
            Text(
                text = title,
                color = AppColors.TextMain,
                fontSize = AppText.Title,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppFont.Serif,
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
        Spacer(modifier = Modifier.height(5.dp))
        // 双细线：上粗下细，报纸 double rule 的经典比例
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(AppColors.TextMain),
        )
        Spacer(modifier = Modifier.height(1.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.Line),
        )
    }
}
