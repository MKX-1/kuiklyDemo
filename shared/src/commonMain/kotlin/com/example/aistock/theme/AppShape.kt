package com.example.aistock.theme

import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 圆角 token。
 *
 * 圆角是「形状语言」的一部分：卡片、标签、弹层各用固定的圆角，
 * 视觉上才有层次感（弹层比卡片更「软」，标签比卡片更「硬」）。
 */
object AppShape {
    /** 卡片：通用容器。 */
    val Card = RoundedCornerShape(12.dp)

    /** 小标签 / 徽章。 */
    val Badge = RoundedCornerShape(4.dp)

    /** 药丸形（Chip、状态角标），取一个远大于高度的值即成正圆角。 */
    val Pill = RoundedCornerShape(999.dp)

    /** 底部弹层：只圆上面两角，因为下面贴着屏幕边。 */
    val SheetTop = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    /** 弹层里的小把手（那条灰色横杠）。 */
    val Handle = RoundedCornerShape(2.dp)
}
