# kuiklyDemo — AI 股票行情 Demo

基于腾讯开源跨端框架 **KuiklyUI**（Kotlin Multiplatform）构建的 AI 股票行情演示 App：
真实行情 → 因子计算 → 大模型分析 → 三级呈现，全链路跑通。

> 免责声明：本项目的行情与 AI 结论仅用于技术演示，不构成任何投资建议。

## 功能

- **自选行情**：沪深/港股真实行情（腾讯行情源），市场维度切换、分组展示、下拉刷新
- **个股详情**：分时 / 60分 / 日K / 周K / 月K 蜡烛图（Canvas 自绘，支持十字光标与跨度缩放）
- **AI 分析**：阿里云百炼 qwen 大模型基于当日行情生成评分/建议/风险文案，
  规则引擎自动兜底，结论来源在界面如实标注
- **跨端架构**：业务代码全部在 KMP `shared` 模块，可扩展至 iOS / 鸿蒙 / H5
- **数据契约**：客户端零解析、金额统一「分」、数据来源三态（实时/缓存/示例）在 UI 如实可见

## 架构

```
androidApp/   Android 宿主壳（KuiklyRenderActivity + 路由/图片/日志等适配器）
shared/       KMP 业务库：theme 设计令牌 → components/core 原子组件
              → components 业务组件 → pages 页面 → data 数据层
backend/      独立 Ktor 服务：腾讯行情代理 + 归一化解析 + 因子计算 + LLM 分析
```

数据流：`腾讯行情 → backend 归一化 → 客户端渲染`；
后端不可用时客户端自动降级：直连腾讯 → 本地样例数据（界面明确标注当前来源）。

## 环境要求

| 依赖 | 版本 |
|---|---|
| JDK | 17（Gradle 必须跑在 17 上，`gradle.properties` 已锁） |
| Android SDK | Platform 35 + Build-Tools 35 |
| Android Studio | Narwhal 及以上（或仅命令行 + SDK） |
| 百炼 API Key | 可选——不配则 AI 分析自动退化为规则引擎 |

> 换机器注意：`gradle.properties` 里 `org.gradle.java.home` 指向本机 JDK17 路径，需改成你的。

## 快速开始

### 1. 启动后端

```bash
cd backend
# Windows
gradlew.bat installDist
# 之后任选其一运行：
#   a) build\install\aistock-backend\bin\aistock-backend.bat
#   b) gradlew.bat run
# macOS/Linux
./gradlew installDist && ./backend/build/install/aistock-backend/bin/aistock-backend
```

可选：启用大模型分析（默认走规则引擎）：

```bash
set DASHSCOPE_API_KEY=sk-你的key        # 阿里云百炼
set LLM_MODEL=qwen-turbo
```

### 2. 运行 App

Android Studio 打开仓库根目录，运行配置选 `androidApp`；或命令行：

```bash
gradlew.bat assembleDebug
adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

真机调试后端连通（模拟器自动走 10.0.2.2）：

```bash
adb reverse tcp:8080 tcp:8080
```

App 右上角角标会标明当前数据来源：绿色「实时」= 走自建后端。

## 后端接口

| 接口 | 说明 |
|---|---|
| `GET /health` | 探活（客户端用它自动发现后端地址） |
| `GET /watchlist?codes=sh600519,hk00700` | 自选行情 + AI 摘要 |
| `GET /analysis/{token}` | 单股 AI 分析（配置 key 时走大模型，否则规则引擎） |
| `GET /chart?token={token}&period=minute\|m60\|day\|week\|month` | 分时 / K 线 |

## 数据契约要点

- **客户端零解析**：腾讯的 `~` 分隔协议、GBK 编码、字段位号全部留在服务端，客户端只消费归一化 JSON
- **单位即契约**：价格/涨跌一律「分」（Long）、市值「元」（Long）——不用浮点存钱
- **三态可见**：LIVE / CACHE / OFFLINE 在 UI 明确标注；样例数据绝不伪装成实时行情
- **结论来源可见**：大模型分析标注「大模型（qwen）」，规则兜底标注「规则引擎」

## 已知限制

- 腾讯行情为未签约公开接口，字段位号可能变化（变化时解析返回空，界面显示「暂无数据」而非错误数据）
- 行业映射为静态表（7 只自选覆盖）；LLM 结论为单日因子推导，不代表真实投研
- 仅实现 Android 端运行；shared 业务代码本身跨端，iOS/H5 宿主未包含

## 目录

```
shared/       业务代码（页面/组件/数据层/设计令牌）
androidApp/   Android 宿主壳
backend/      Ktor 后端（独立 Gradle 工程）
tools/        模拟器一键启动与保活脚本（可选，Windows）
docs/         架构说明与截图
```
