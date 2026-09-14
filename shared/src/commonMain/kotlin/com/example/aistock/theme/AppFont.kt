package com.example.aistock.theme

import com.tencent.kuikly.compose.ui.text.font.FontFamily

/**
 * 字族 token —— 文艺风的核心不在颜色，在**字体气质**。
 *
 * 三族分工（来自 impeccable/typography 的配对原则：衬线 + 无衬线才有结构对比）：
 *
 *  - [Serif]（衬线，Android 上映射系统 serif / 中文宋体系）——
 *    用于**标题与价格数字**。宋体的横细竖粗天生带「报刊标题」的仪式感，
 *    这套文艺风成不成立，八成看它；
 *  - [Sans]（无衬线）——正文与标签。小字号下无衬线更清晰，
 *    而且衬线/无衬线的交替本身就是层级信号；
 *  - [Mono]（等宽）——表格型小数字（盘口网格），数字等宽才能上下对齐，
 *    这是报刊行情栏的传统排法。
 *
 * ⚠️ 只用系统字族，不引自定义字体文件：不增加包体，且跨端（iOS/鸿蒙/H5）
 * 都有对应系统衬线可用。FontFamily.Serif/Monospace 在本 fork 已确认存在
 * （FontFamily.kt: GenericFontFamily("serif" / "monospace")）。
 */
object AppFont {
    /** 标题、价格数字、刊头 —— 报刊气质的担当。 */
    val Serif = FontFamily.Serif

    /** 正文、标签、说明文字。 */
    val Sans = FontFamily.SansSerif

    /** 网格化小数字（盘口、统计格），等宽保证纵向对齐。 */
    val Mono = FontFamily.Monospace
}
