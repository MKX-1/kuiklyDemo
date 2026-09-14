# AI 股票行情 Demo（Kuikly 跨端）

参考 Kuikly 开源示例 `tsusinai/kuikly-todo` 的架构搭建：**Kuikly 客户端（KMP）+ 自建 Ktor 后端 + 腾讯行情数据源**。

> 架构讲解（含技术栈扫盲）→ [`docs/architecture.md`](docs/architecture.md)

## 目录结构

```
ai-stock-demo/
├── shared/                 # ★ 跨平台业务库，业务代码全在这里
│   └── src/commonMain/kotlin/com/example/aistock/
│       ├── pages/          页面：WatchlistPage（自选行情）+ WatchlistViewModel
│       ├── components/     可复用组件：StockCard / MarketOverviewBar
│       ├── data/           数据层：降级链 / 腾讯解析 / 规则引擎 / 格式化
│       └── theme/          颜色与字号 token
├── androidApp/             # Android 宿主壳（Kuikly 容器 + 适配器）
├── backend/                # 自建后端（Kotlin + Ktor，独立 Gradle 工程）
│   └── src/main/kotlin/com/example/aistock/backend/
│       ├── route/          接口：/health /watchlist /analysis/{token}
│       ├── service/        流程编排
│       ├── parse/          腾讯 ~ 协议解析
│       ├── ai/             规则引擎（预留 LLM 接缝）
│       └── client/         上游抓取（GBK 解码）
├── docs/architecture.md    # 架构说明 + 技术栈解释
└── gradle.properties       # 锁 JDK17（Kuikly 硬性要求）
```

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

> 不起后端也能跑：客户端会降级到「直连腾讯」，Android 上能直接看到真实行情。

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

界面右上角有**数据来源角标**，三态可见，不会把示例数据伪装成实时行情：

| 角标 | 含义 |
|---|---|
| 实时 | 真实行情（后端或直连腾讯） |
| 非实时 HH:mm:ss | 本轮刷新失败，展示的是上次缓存的数据 |
| 示例数据 | 网络与后端都不可用，展示的是演示占位数据（非真实行情） |

行情与 AI 结论**仅用于功能演示，不构成任何投资建议**。
