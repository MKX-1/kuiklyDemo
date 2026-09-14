package com.example.aistock.theme

import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.sp

/**
 * 颜色 token —— 视觉方向：**纸墨行情 / 中文财经报刊**。
 *
 * 定调说明（为什么从「深空终端」换成「纸墨报刊」）：
 *  - 文艺气质的来源不是颜色本身，而是**材质感**：宣纸暖白 + 浓墨文字，
 *    像一张摊开的财经报纸。深底霓虹是「科技感」，纸墨才是「文气」；
 *  - 配色仍按 60/30/10：纸色占 60%，墨色文字占 30%，
 *    **单一强调色（黛青 #2F5D62）占 10%** —— 用于 AI、选中态、数据高亮。
 *    黛青是传统国画用色，比科技青沉稳，和纸色是同一个「文气」体系；
 *  - 涨跌色是**语义色**不计入 10%：涨用**朱砂**（印章红，偏沉的红，
 *    比荧光红有文气），跌用**黛绿**。红涨绿跌的中国市场惯例不动。
 *
 * 对比度：浓墨 #2B2519 在宣纸上对比度 > 12:1；朱砂/黛绿用于大号数字，均 ≥ 4.5:1。
 */
object AppColors {

    // ---------- 底层（60% 主色域）----------
    /** 页面底：宣纸暖白。比纯白黄一点，才有「纸」的感觉。 */
    val PageBg = Color(0xFFF5F0E6)

    /** 面板（卡片）底：比页面纸更亮一档，像贴上去的一页新纸。 */
    val Panel = Color(0xFFFCF9F1)

    /** 面板高亮层：选中/展开态。 */
    val PanelHi = Color(0xFFFFFDF6)

    /** 顶部报头底：略深的旧纸色，跟内容区做微妙区分。 */
    val HeaderBg = Color(0xFFEEE7D7)

    /** 描边：淡墨线。报刊的框架感全靠它，不用阴影。 */
    val Line = Color(0xFFD8CDB4)

    /** 弱描边：行间分隔线、次要轮廓。 */
    val LineSoft = Color(0xFFE4DBC7)

    // 兼容旧命名（早期代码用 CardBg/Border，语义相同）
    val CardBg = Panel
    val Border = Line

    // ---------- 文字（30% 辅助域）----------
    val TextMain = Color(0xFF2B2519)
    val TextSub = Color(0xFF6E6350)
    val TextWeak = Color(0xFFA2957D)

    // ---------- 强调（10%，唯一）----------
    /** 主强调色：黛青（国画用色）。AI、选中、数据高亮只用它。 */
    val Accent = Color(0xFF2F5D62)

    /** 强调色深版，用于渐变收尾。 */
    val AccentDim = Color(0xFF1E4045)

    /** 强调色的极淡填充（选中态背景）。 */
    val AccentSoft = Color(0x1A2F5D62)

    /** 兼容旧命名：Primary / AiAccent 统一收敛到唯一强调色。 */
    val Primary = Accent
    val AiAccent = Accent

    // ---------- 语义色（涨跌，不计入 10%）----------
    /** ⚠️ 中国市场惯例：红涨绿跌。涨=朱砂（印章红），跌=黛绿。 */
    val Up = Color(0xFFC14B3A)
    val Down = Color(0xFF2F7D5D)

    val Warning = Color(0xFFB07D2B)

    // ---------- 氛围层 ----------
    /** 背景纹样线（墨 3% 透明）：纸面的极淡肌理，不抢内容。 */
    val GridLine = Color(0x082B2519)

    /** 氛围层（墨 2%），只在报头区域用一点。 */
    val Scan = Color(0x052B2519)

    /** 涨跌 → 颜色。全项目共用这一份判断，避免某处红绿写反。 */
    fun ofChange(changePct: Double): Color = when {
        changePct > 0 -> Up
        changePct < 0 -> Down
        else -> TextSub
    }
}

/**
 * 字号 token。Kuikly 的 fontSize 是 TextUnit（sp 单位），裸 Float 编译不过。
 *
 * 排印方法（来自 impeccable/typography 的模块化比例思想）：
 * **字号少而对比大**。全 App 只有 7 档，相邻档位差 ≥ 2sp，
 * 层级靠「字号 + 字重 + 字族 + 颜色」四个维度叠加，不靠微调字号。
 */
object AppText {
    /** 详情页主价格，一屏最大。衬线加粗才有报刊标题的味道。 */
    val HeroPrice = 36.sp

    /** 列表里的现价。 */
    val Price = 20.sp

    /** 价格变动（小一号，跟在现价后面）。 */
    val Delta = 13.sp

    val Title = 17.sp
    val Body = 14.sp
    val Small = 13.sp

    /** 小节标题、标签：小号 + 靠颜色与字重分层。 */
    val Caption = 12.sp
    val Tiny = 11.sp

    /** 刊头注脚（最小一级，只用于装饰性信息）。 */
    val Micro = 10.sp
}
