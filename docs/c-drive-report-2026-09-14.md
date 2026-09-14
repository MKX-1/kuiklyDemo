# C 盘空间排查报告

**排查时间**：2026-09-14 14:2x（北京时间）
**触发问题**：运行 Android 模拟器期间，C 盘可用空间急剧减少约 10 GB

---

## 一、结论先说

**主因不是模拟器。** 那 10 GB 是「Windows 更新包 + 开发工具链缓存」叠加的结果，其中最大的一块（10.78 GB）是 Windows 更新下载/解包缓存，模拟器本身只占约 3.5 GB。

时间上确实和跑模拟器重合，因为两件事都发生在你那次重启前后（你在 Android Studio 里启用 WHPX → 重启 → Windows Update 开始下载/安装 + 我同时装模拟器跑 App）。

---

## 二、C 盘总体

| 项 | 数值 |
|---|---|
| 总容量 | 296.39 GB |
| 已用 | 271.22 GB |
| **可用** | **25.16 GB** |

各盘对比：C 25.16 GB / D 286.00 GB / E 266.85 GB（E 盘空间充裕，是搬家的理想落点）

---

## 三、占用排行（实测）

| 项目 | 大小 | 文件数 | 性质 |
|---|---:|---:|---|
| `C:\Windows\SoftwareDistribution\Download` | **11.12 GB** | 735,426 | Windows 更新包缓存 |
| `C:\Users\rog\.android\avd\aistock.avd` | 3.53 GB | 25 | 模拟器镜像 + 快照 |
| `C:\Users\rog\.gradle` | 3.34 GB | 34,786 | Gradle 依赖缓存 |
| `C:\Windows\Installer` | 2.29 GB | 321 | MSI 安装缓存 |
| `C:\$WinREAgent` | 1.86 GB | 9 | 更新/恢复环境残留 |
| `C:\Users\rog\AppData\Local\Android\Sdk` | 1.48 GB | 28,375 | **重复的 SDK 副本** |
| `C:\Users\rog\AppData\Local\Temp` | 1.16 GB | 1,244 | 临时文件 |
| `C:\Users\rog\AppData\Local\Google` | 1.05 GB | 2,647 | Android Studio 索引缓存 |
| `C:\Users\rog\AppData\Local\Packages` | 1.04 GB | 6,693 | UWP 应用数据 |
| `C:\$Recycle.Bin` | 0.49 GB | 6,910 | 回收站 |
| `C:\$WINDOWS.~BT` | 0.20 GB | 573 | 更新安装残留 |

### 更新包细节

`Download` 目录里几乎全部体积来自**单个目录** `1105ffd0dd49d0a2225cbc521c2a3c95`（**10.78 GB**），内容为 Windows 11 累积/功能更新包：

- `Microsoft-Windows-Client-Desktop-Required-Package.ESD` 777 MB
- `corecountryspecific_zh-cn.esd` 648 MB
- `Microsoft-Windows-Client-Features-Package.ESD` 250 MB
- `Microsoft-Edge-WebView-FOD-...cab` 194 MB
- `Windows11.0-KB5124015-x64.cab` 163 MB
- `Microsoft-Win4-Feature.ESD` 161 MB
- `Windows11.0-KB5124008-x64.mumx.esd` 110 MB ×2
- `Package_for_RollupFix~~amd64~~26100.9445.1.26\...\mpasbase.vdm` 128 MB

按创建日分布：**09-13 占 7.89 GB，09-14 占 1.97 GB**（其余为更早的零星文件）。排查时 `wuauserv` 与 `TrustedInstaller` 仍在运行，无待重启标志，系统版本 `Windows 11 家庭版 build 26200`。

---

## 四、今天新增的空间去哪了（约 11.9 GB）

| 来源 | 今日新增 | 说明 |
|---|---:|---|
| 模拟器 | 3.53 GB | AVD 的 `ram.img` 开机快照 2.56 GB + `userdata-qemu.img.qcow2` 0.96 GB |
| 两个项目的构建 | 2.70 GB | Gradle 下载 Kotlin/AGP/Ktor 等依赖（kuikly-demo + ai-stock-demo） |
| Android Studio | 2.35 GB | 自建 SDK 副本 1.48 GB + 索引缓存 0.87 GB |
| Windows 更新 | 2.17 GB | 更新缓存今日新建 1.97 GB + `$WINDOWS.~BT` 0.20 GB |
| 临时文件 | 1.16 GB | 构建日志、扫描中间产物等 |
| **合计** | **≈11.9 GB** | 与「掉了 10 GB」的体感吻合 |

---

## 五、可清理候选（尚未执行任何操作）

> ⚠️ **此操作非常危险，可能导致不可逆的数据丢失！以下仅为清单，我没有删除任何文件。**

| 候选 | 可回收 | 风险 | 建议 |
|---|---:|---|---|
| `snapshots\default_boot\ram.img`（AVD 开机快照） | 2.56 GB | 低 | 关掉模拟器时不保存快照即可删；代价是下次冷启动慢些 |
| `C:\$WinREAgent` | 1.86 GB | 低 | 更新残留，Windows 通常 10 天后自动删 |
| `C:\$WINDOWS.~BT` | 0.20 GB | 低 | 同上 |
| `%TEMP%` 内容 | 1.16 GB | 低 | 临时文件 |
| 回收站 | 0.49 GB | 低 | 清空前请自行确认无待恢复文件 |
| `AppData\Local\Android\Sdk`（重复副本） | 1.48 GB | **中** | 需先确认 Android Studio 的 SDK 路径设置；若 Studio 指向它，删除会破坏 IDE |
| Gradle 旧版本缓存 | 1～2 GB | 中 | 清了下次构建要重新下载 |
| `SoftwareDistribution\Download` | 11.12 GB | **高** | `wuauserv`/`TrustedInstaller` 正在运行，更新可能未完成，**现在不要删**；应等更新结束后用系统「磁盘清理」或 DISM 处理 |

**小计（低风险项）**：约 6.3 GB

---

## 六、长期方案：把开发缓存搬到 E 盘

当前环境变量（用户级注册表）：

| 变量 | 当前值 |
|---|---|
| `ANDROID_HOME` / `ANDROID_SDK_ROOT` | `C:\Users\rog\Documents\Codex\Android\Sdk` |
| `ANDROID_USER_HOME` | `C:\Users\rog\Documents\Codex\Android\UserHome` |
| `GRADLE_USER_HOME` | 未设置（默认 `C:\Users\rog\.gradle`） |

建议（E 盘剩余 266.85 GB，足够）：

1. **AVD 搬走**：设 `ANDROID_AVD_HOME=E:\android\avd`，把 `C:\Users\rog\.android\avd` 整体移过去。可省 3.5 GB 且不再回涨。
2. **Gradle 缓存搬走**：设 `GRADLE_USER_HOME=E:\gradle-home`。可省 3.3 GB 且不再回涨。
3. **SDK 搬家（可选）**：把 `Documents\Codex\Android\Sdk` 移到 `E:\Android\Sdk`，同步改上述两个环境变量 + 各项目 `local.properties`。可再省 5.5 GB。
4. **禁用模拟器快照保存**：启动参数加 `-no-snapshot`，或在 AVD 配置里关闭快照。

> 注：迁移 AVD / Gradle 缓存属于「移动 + 改环境变量」，不涉及删除个人文件；但 SDK 搬家会动到 Android Studio 的配置，需要改完在 Studio 里重新指认一次 SDK 路径。

---

## 七、排查方法备注

- 先用 PowerShell `Get-ChildItem -Recurse` 扫 `SoftwareDistribution\Download`，73 万文件**跑了 17 分钟没结束**（已终止）。
- 改用 Python `os.scandir` 递归统计，全量 29 秒完成。脚本：`C:\Users\rog\AppData\Local\Temp\fastscan.py`。
- 模拟器进程 `qemu-system-x86_64.exe`（PID 25036）在排查时仍在运行，占用约 4 GB 内存。
