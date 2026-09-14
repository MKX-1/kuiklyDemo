package com.example.aistock.theme

import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.sp

/**
 * 颜色 token —— 视觉方向：**深空交易终端 / HUD 仪表盘**。
 *
 * 定调说明（为什么是这套色）：
 *  - 股票行情本质是「高密度数据终端」，不是社交产品。深底能让红绿数字最亮、最跳，
 *    浅底反而会跟涨跌色抢注意力；
 *  - 配色按 60/30/10 分配：深空底色占 60%，冷灰蓝文字占 30%，
 *    **单一强调色（青 #2CE5E0）占 10%** —— 只用于数据高亮、AI 相关描边和动效。
 *    单页只有一个强调色是刻意的：多色强调会立刻变成「AI 味」的花哨；
 *  - 涨跌色（红/绿）**不计入** 10%，它们是**语义色**而非装饰色 ——
 *    用户扫一眼颜色就知道涨跌，这里必须遵守中国市场惯例：**红涨绿跌**。
 *
 * 但它在深底上要提亮一档：浅底用的正红正绿放到近黑底上会发闷，
 * 所以换成霓虹红/荧光绿，对比度才够（正文对比度目标 ≥ 4.5:1）。
 */
object AppColors {

    // ---------- 底层（60% 主色域）----------
    /** 页面底：近黑的深空蓝，比纯黑柔和，给氛围图层留出层次。 */
    val PageBg = Color(0xFF05070D)

    /** 面板（卡片）底：比页面底亮一档，形成「浮起」感。 */
    val Panel = Color(0xFF0C1220)

    /** 面板高亮层：用于选中/展开，再亮一档。 */
    val PanelHi = Color(0xFF131C2E)

    /** 顶部 header 底：略偏蓝，让它跟内容区有微妙区分。 */
    val HeaderBg = Color(0xFF0A0F1A)

    /** 描边：面板的 1px 边框。深色设计里「边框」比「阴影」更能表达层次。 */
    val Line = Color(0xFF1E2A40)

    /** 弱描边：分隔线、次要轮廓。 */
    val LineSoft = Color(0xFF16202F)

    // 兼容旧命名（早期代码用 CardBg/Border，语义相同）
    val CardBg = Panel
    val Border = Line

    // ---------- 文字（30% 辅助域）----------
    val TextMain = Color(0xFFE8F0FB)
    val TextSub = Color(0xFF8797B0)
    val TextWeak = Color(0xFF4E5C74)

    // ---------- 强调（10%，唯一）----------
    /** 主强调色：青。数据高亮、AI 描边、动效都只用它。 */
    val Accent = Color(0xFF2CE5E0)

    /** 强调色的低亮版本，用于渐变收尾、发光边缘。 */
    val AccentDim = Color(0xFF0E6C6C)

    /** 强调色的极淡填充（选中态背景），透明度低到不抢文字。 */
    val AccentSoft = Color(0x1A2CE5E0)

    /** 兼容旧命名：Primary / AiAccent 统一收敛到唯一强调色。 */
    val Primary = Accent
    val AiAccent = Accent

    // ---------- 语义色（涨跌，不计入 10%）----------
    /** ⚠️ 中国市场惯例：红涨绿跌（与欧美市场相反）。 */
    val Up = Color(0xFFFF4B6E)
    val Down = Color(0xFF12E29A)

    val Warning = Color(0xFFFFB020)

    // ---------- 氛围层 ----------
    /** 背景网格线（白 4% 透明）：制造「仪表盘」的空间感，不抢内容。 */
    val GridLine = Color(0x0AFFFFFF)

    /** 扫描线（白 2%）：CRT 质感，只在 header 区域用一点。 */
    val Scan = Color(0x05FFFFFF)

    /** 涨跌 → 颜色。全项目共用这一份判断，避免某处红绿写反。 */
    fun ofChange(changePct: Double): Color = when {
        changePct > 0 -> Up
        changePct < 0 -> Down
        else -> TextSub
    }
}

/**
 * 字号 token。Kuikly 的 fontSize 是 TextUnit（sp 单位），
 * 用裸 Float 会直接编译不过。
 *
 * 数值取向：数值类文字整体比正文大 2–4sp，因为在行情界面里「数字才是主角」。
 */
object AppText {
    /** 详情页主价格，一屏最大。 */
    val HeroPrice = 34.sp

    /** 列表里的现价。 */
    val Price = 19.sp

    /** 价格变动（小一号，跟在现价后面）。 */
    val Delta = 13.sp

    val Title = 16.sp
    val Body = 14.sp
    val Small = 13.sp

    /** 小节标题、标签：小号 + 靠颜色而非字号分层。 */
    val Caption = 12.sp
    val Tiny = 11.sp

    /** 仪表面板上的刻度标注（最小一级，只用于装饰性信息）。 */
    val Micro = 10.sp
}
