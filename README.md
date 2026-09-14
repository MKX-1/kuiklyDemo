# AI 股票行情 Demo（Kuikly 跨端）

**Kuikly 客户端（KMP）+ 自建 Ktor 后端 + 腾讯行情数据源。**

业务代码 100% 写在 `shared/commonMain`，Android / iOS / 鸿蒙 / H5 共用同一份；
各端只做薄宿主接入。当前已跑通 Android。

> 架构讲解（含客户端技术栈扫盲）→ [`docs/architecture.md`](docs/architecture.md)
> 设计方向预览（深空终端视觉）→ [`docs/design-preview.html`](docs/design-preview.html)

## 目录结构

```
ai-stock-demo/
├── shared/                     # ★ 跨平台业务库，业务代码全在这里
│   └── src/commonMain/kotlin/com/example/aistock/
│       ├── pages/              页面（WatchlistPage 自选行情 + ViewModel）
│       ├── components/         业务组件（StockCard / MarketOverviewBar）
│       │   └── core/           原子组件（Panel / Chip / Tag / Reveal / StateBox / Glyph …）
│       ├── data/               数据层：接口 + 三个实现 + 降级路由 + 解析 + 规则引擎
│       └── theme/              设计令牌：颜色 / 字号 / 间距 / 圆角 / 动效时长
├── androidApp/                 # Android 宿主壳（Kuikly 容器 + 图片/日志/路由/线程/异常适配器）
├── backend/                    # 自建后端（Kotlin + Ktor，独立 Gradle 工程）
│   └── src/main/kotlin/com/example/aistock/backend/
│       ├── route/              接口：/health /watchlist /analysis/{token}
│       ├── service/            流程编排
│       ├── parse/              腾讯 ~ 协议解析
│       ├── ai/                 规则引擎（预留 LLM 接缝）
│       ├── dto/ model/         传输与领域模型
│       └── client/             上游抓取（GBK 解码）
├── tools/                      本地脚本：模拟器启停与保活、行情源自检、目录占用统计
├── docs/                       架构说明 + 设计方向预览
├── .gitattributes              统一换行符（源码 LF，*.bat 保留 CRLF）
└── gradle.properties           锁定 JDK17（Kuikly 硬性要求）
```

## 架构要点

**分层**：设计令牌 → 原子组件 → 业务组件 → 页面（只做组装与状态分发）→ 数据层。
页面不直接碰网络，数据层不感知 UI。

**数据契约**：

- **客户端零解析**：腾讯的字段位号、GBK 解码、股票名称、行业映射全部留在服务端，客户端只消费归一化 JSON；
- **单位统一**：价格一律「分」(Long)、市值一律「元」(Long) —— 浮点存钱必然产生误差；
- **三态可见**：实时 / 非实时 / 示例数据必须在界面上明说，绝不把兜底数据伪装成真实行情；
- **降级链**：自建后端 → 直连腾讯 → 本地示例数据，任一层挂掉都不会白屏。

## 设计系统

视觉方向为「深空交易终端 / HUD 仪表盘」：

- 深底 + 面板 + 1px 描边表达层次（深色界面上阴影几乎不可见，所以不用阴影）；
- **单一强调色**（青 `#2CE5E0`）同时承担 AI、选中、数据高亮三重语义，靠形状与描边区分；
- 涨跌使用霓虹红绿，并保留中国市场惯例：**红涨绿跌**；
- 动效：列表 45ms 错峰入场、实时角标呼吸指示、网格与扫描线氛围层。

## 快速开始

环境要求：JDK 17、Android SDK（platform 35）。Gradle 用自带 wrapper，无需安装。

### 1. 后端（可选，但推荐）

```bash
cd backend
gradlew.bat run            # 默认 http://127.0.0.1:8080
```

自测：

```bash
curl "http://127.0.0.1:8080/health"
curl "http://127.0.0.1:8080/watchlist?codes=sh600519,hk00700,sz300750"
curl "http://127.0.0.1:8080/analysis/sh600519"
```

### 2. 客户端

```bash
gradlew.bat assembleDebug    # 产物：androidApp/build/outputs/apk/debug/androidApp-debug.apk
gradlew.bat installDebug     # 装到已连接的手机
```

手机连本机后端的三种方式（客户端已内置候选地址自动探测）：

| 场景 | 做法 |
|---|---|
| 真机 + USB（推荐） | `adb reverse tcp:8080 tcp:8080` |
| Android 模拟器 | 无需操作，客户端会自动试 `10.0.2.2:8080` |
| 局域网 | 把 `http://<电脑IP>:8080` 加到 `BackendConfig.hostCandidates` 首位 |

> 不起后端也能跑：客户端会降级到「直连腾讯」，Android 上仍能看到真实行情。

## 版本约定

| 组件 | 版本 |
|---|---|
| Kuikly | 2.27.0-2.1.21（坐标规则 `${框架版本}-${Kotlin版本}`） |
| Kotlin / KSP | 2.1.21 / 2.1.21-2.0.1 |
| AGP / Gradle | 8.7.3 / 8.9 |
| compileSdk / minSdk | 35 / 24 |
| JDK | 17（Gradle 必须跑在 17 上） |

> Kuikly 的依赖只发布在腾讯源 `https://mirrors.tencent.com/nexus/repository/maven-tencent/`，
> 已在 `settings.gradle.kts` 配好。

## 数据可靠性（重要）

界面右上角有**数据来源角标**，三态可见：

| 角标 | 含义 |
|---|---|
| 实时 | 真实行情（后端或直连腾讯） |
| 非实时 HH:mm:ss | 本轮刷新失败，展示的是上次缓存的数据 |
| 示例数据 | 网络与后端都不可用，展示的是演示占位数据（非真实行情） |

行情与 AI 结论**仅用于功能演示，不构成任何投资建议**。市场有风险，投资需谨慎。
