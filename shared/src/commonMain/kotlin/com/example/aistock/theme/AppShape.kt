package com.example.aistock.theme

import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 圆角 token —— 形状语言。
 *
 * 纸墨风用**近直角**（2dp）：纸张是方的，圆角一大就变「互联网卡片」，
 * 文气全无。留 2dp 是为了描边在直角处不发虚。
 */
object AppShape {
    /** 卡片：通用容器。纸卡近直角。 */
    val Card = RoundedCornerShape(2.dp)

    /** 小标签 / 徽章。 */
    val Badge = RoundedCornerShape(2.dp)

    /** 药丸形（Chip、状态角标），取一个远大于高度的值即成正圆角。 */
    val Pill = RoundedCornerShape(999.dp)

    /** 底部弹层：只圆上面两角，因为下面贴着屏幕边。 */
    val SheetTop = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)

    /** 弹层里的小把手（那条灰色横杠）。 */
    val Handle = RoundedCornerShape(2.dp)
}
