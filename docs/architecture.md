# AI 股票行情 Demo — 架构说明

> 本项目参考 Kuikly 开源示例工程 `tsusinai/kuikly-todo` 的架构组织方式搭建。
> 本文面向「没做过客户端开发」的读者，先把名词解释清楚，再讲设计为什么这么定。

---

## 一、一句话看懂整体架构

```
┌─────────────────────────────────────────────────────────────┐
│  客户端（跑在手机上）                                          │
│  shared/  ← Kuikly / Kotlin Multiplatform                    │
│    pages/       页面（自选行情页）                             │
│    components/  可复用 UI 组件（股票卡片、大盘摘要条）            │
│    data/        数据层（降级链、规则引擎、格式化）                │
│    theme/       颜色字号等设计 token                           │
│  androidApp/   ← Android 宿主壳（把上面的 UI 装进 Android App）  │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP / JSON
┌───────────────────────────▼─────────────────────────────────┐
│  自建后端 backend/（跑在你电脑上，Kotlin + Ktor）                │
│    route/    接口定义  /health /watchlist /analysis/{token}    │
│    service/  流程编排（取数 → 解析 → 富化 → AI 打分 → 组装）      │
│    parse/    解析腾讯的 ~ 分隔协议（脏活都在这）                  │
│    ai/       规则引擎（评分 / 标签 / 摘要），预留 LLM 接缝         │
│    client/   上游抓取（GBK 解码）                              │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP
                    ┌───────▼────────┐
                    │ 腾讯行情接口     │
                    │ qt.gtimg.cn    │
                    └────────────────┘
```

**为什么中间要加一个自己的后端？** 三个真实理由：

1. **脏活集中**：上游返回的是 `~` 分隔的 GBK 文本，字段散落在几十个下标里，还有单位不一致（A 股成交额单位是「万」、港股是「元」）。这些全放后端做一次，客户端就只用搬干净的 JSON。
2. **密钥/逻辑可控**：以后接真大模型，API Key 必须放服务端；AI 分析的规则改动也不用发新版 App。
3. **可降级**：后端不可用时，客户端会退到「直连腾讯」，再不行才用本地示例数据——用户永远有东西可看，而且能看到自己看的是什么。

---

## 二、数据源的降级链（本项目最重要的设计）

```
第 1 级  自建后端          → 有数据就用，来源标 LIVE
   ↓ 后端没起 / 挂了
第 2 级  直连腾讯 qt.gtimg.cn → 有数据就用，来源标 LIVE
   ↓ 网络也不通
第 3 级  本地示例数据        → 来源标 OFFLINE，界面打横幅明示「展示的是示例数据」
```

另外还有一个**缓存态（CACHE）**：本来有数据，这次刷新失败了 → 保留旧数据不清空，
界面显示「行情非实时（更新于 10:32:11）」。

三态在 UI 上是**可见的**（右上角角标 + 横幅）：

| 状态 | 含义 | 界面表现 |
|---|---|---|
| `LIVE` | 实时行情 | 绿色「实时」角标 |
| `CACHE` | 上次的数据，本轮没刷新成功 | 橙色「非实时 10:32:11」 |
| `OFFLINE` | 示例数据（不是真行情） | 橙色「示例数据」+ 横幅提示 |

> **这一条是红线**：把示例数据伪装成实时行情，在金融场景里是不可接受的。
> 宁可告诉用户「现在只能看示例」，也不能让他以为那是真实价格。

---

## 三、分层与职责

| 层 | 目录 | 只负责任么 | 不允许做 |
|---|---|---|---|
| 页面 | `pages/` | 组装组件、响应用户操作 | 直接发网络请求、写业务规则 |
| 组件 | `components/` | 输入数据 → 输出 UI | 持有业务状态 |
| 数据 | `data/` | 取数、降级、推导、格式化 | 依赖任何 UI 概念（Activity/颜色） |
| 主题 | `theme/` | 颜色 / 字号 token | 写业务逻辑 |

**为什么要这么分？** 最直观的好处：`data/` 里全是纯 Kotlin，没有一行 Android 代码，
所以它能在 Android / iOS / 鸿蒙 / 浏览器上跑同一份；也能脱离 UI 单独写单元测试。

---

## 四、一次「下拉刷新」在代码里发生了什么

```
1. UI    WatchlistScreen 里点击横幅重试 → 调 vm.load()
2. VM    WatchlistViewModel.load() → 启动协程，调 api.fetchWatchlist()
3. 数据层 RoutedStockApi：
          ① 先问 BackendApi（探活 /health → GET /watchlist?codes=...）
          ② 失败 → TencentStockApi 直连 qt.gtimg.cn 自己解析
          ③ 还失败 → SampleStockApi（示例数据）
4. 后端   接 ① 时：TencentClient 抓取(GBK 解码) → QuoteParser 解析
          → 补行业/振幅/基准差/标签 → RuleEngineAiEngine 打分 → 输出 JSON
5. VM    拿到 WatchlistBundle（含 stocks / source / fetchedAt）
         把状态写进 mutableStateOf
6. UI    Compose 监听到状态变化 → 自动重画变化的部分（不用手写刷新代码）
```

第 6 步是 Compose 这类**声明式 UI** 的关键：你只描述「数据长这样时界面应该长什么样」，
数据变了框架自己重画。相比老式的「找到那个 View 再手动 setText」，能省掉大量同步 bug。

---

## 五、技术栈扫盲（写给不懂客户端的人）

| 名词 | 一句话解释 | 在本项目里的体现 |
|---|---|---|
| **Kotlin** | JVM 上的一门现代语言，写起来比 Java 短很多 | 全项目语言 |
| **KMP**（Kotlin Multiplatform） | 一套 Kotlin 代码编译成多个平台的原生产物（Android 的 aar、iOS 的 framework、鸿蒙的 so、H5 的 js） | `shared/` 模块就是跨平台的 |
| **Kuikly** | 腾讯开源的跨端 UI 框架，基于 KMP。UI 在原生控件上渲染（不是 WebView），所以性能接近原生 | `shared/` 里的页面 |
| **Compose DSL** | Google 的声明式 UI 写法（Jetpack Compose）。Kuikly 把它移植成跨端版，包名是 `com.tencent.kuikly.compose.*` | `WatchlistPage.kt` 里全是 `@Composable` 函数 |
| **Gradle** | 构建工具：管依赖、编译、打包。`build.gradle.kts` 就是它的配置文件 | 根目录与各模块的 `build.gradle.kts` |
| **Gradle Wrapper** | 项目自带的 Gradle 启动器（`gradlew`），保证每个人用同一个 Gradle 版本 | `gradlew` + `gradle/wrapper/` |
| **KSP** | 编译期代码生成器。Kuikly 用它扫描 `@Page` 注解，自动生成页面注册表 | `KuiklyCoreEntry.kt`（自动生成，别手写） |
| **ViewModel** | 存放「页面状态 + 业务逻辑」的容器，页面重画时不丢状态 | `WatchlistViewModel.kt` |
| **单向数据流** | UI 只读状态、只调方法；状态变化只有一个来源（VM） | 数据层 → VM → UI |
| **协程 / `suspend`** | 轻量的异步写法。`suspend` 函数能「暂停等网络」但不阻塞线程 | `fetchWatchlist()` |
| **JSON 序列化** | 对象 ↔ JSON 文本互转。后端用 kotlinx.serialization，客户端用 Kuikly 的 JSON API | `Dtos.kt` / `BackendApi.kt` |
| **Ktor** | JetBrains 出的 Kotlin 服务端框架，轻量 | `backend/` |
| **契约** | 前后端约定的接口形状（字段名、单位、类型） | `Dtos.kt` 与 `StockModels.kt` 一一对应 |

---

## 六、单位与数据口径（金融场景必须钉死）

| 数据 | 类型 | 单位 | 为什么 |
|---|---|---|---|
| 价格 / 涨跌 / 开高低 | `Long` | **分** | 浮点存钱有精度误差（0.1+0.2≠0.3），金融数据必须用整数最小单位 |
| 市值 / 流通市值 / 成交额 | `Long` | **元** | 同上 |
| 涨跌幅 / 换手率 / 振幅 | `Double` | **%** | 本身就是比率 |
| 量比 | `Double` | 倍数 | — |
| `benchmarkDelta` | `Double?` | % | 个股涨跌幅 − 对应指数涨跌幅；**指数缺失时为 null，界面显示「—」，绝不臆造数字** |
| `floatCapDelta` | `Long` | 元 | **Σ 流通市值变动**，注意它**不是**「资金净流入」，两者含义完全不同 |

---

## 七、AI 部分怎么做的（说清楚边界）

**当前是「规则引擎」，不是真正的大模型。** 它只用「当日涨跌幅 + 市盈率」做机械推导，输出四维画像：

- `action` 操作建议：重点关注 / 低吸关注 / 持股观望 / 建议回避
- `signal` 依据信号：量能放大 / MACD金叉 / 低位企稳 / 超跌反弹
- `score` 评分：以 50 分为基准，按动量(0-60) + 价值(0-25) + 风险(0-8) 增减，收敛到 0-100
- `scenario` 场景：建议加自选 / 建议建仓 / 继续持有 / 建议减仓

**接缝已经预留好了**：后端是 `AiEngine` 接口，客户端是 `StockApi` 接口。
将来接真 LLM，只需新增一个实现类，路由层和 UI 一行都不用改。

---

## 八、怎么跑起来

### 后端（可选，但推荐先起）

```bash
cd backend
gradlew.bat run                 # 默认 8080 端口，需要 JDK17
```

验证：

```bash
curl "http://127.0.0.1:8080/health"
curl "http://127.0.0.1:8080/watchlist?codes=sh600519,hk00700"
```

### 客户端

```bash
gradlew.bat assembleDebug       # 产物：androidApp/build/outputs/apk/debug/androidApp-debug.apk
gradlew.bat installDebug        # 或直接装到手机
```

**手机怎么连到你电脑上的后端？** 三选一：

1. 真机 + USB：`adb reverse tcp:8080 tcp:8080`（推荐，客户端已把 `127.0.0.1` 放在首位）
2. 模拟器：客户端会自动尝试 `10.0.2.2`（模拟器里指向宿主机的别名）
3. 局域网：把 `http://<你电脑的IP>:8080` 加到 `BackendConfig.hostCandidates` 第一位

> 不起后端也能跑：客户端会退到「直连腾讯」，Android 上可直接看到真实行情。

---

## 九、当前进度与后续

**已完成（本阶段 = 架构跑通）**
- 客户端骨架：Compose DSL 页面 + MVVM + 三层降级数据层 + 设计 token
- 后端骨架：Ktor 服务 + 腾讯协议解析 + 规则引擎 + 契约
- 全链路真实数据已验证（后端实测返回真实行情）

**待做（后续阶段）**
- 个股详情页 + K 线图组件（周期/样式/指标可切）
- 下拉刷新（`pullToRefreshItem`）、列表错误态重试
- AI 分析弹层（本地先出、后端校正）
- 自选列表本地持久化（`SharedPreferencesModule`）
- 接真 LLM（实现 `AiEngine` 的 LLM 版本）
- iOS / 鸿蒙宿主工程（需要在 Mac / DevEco 环境验证）
