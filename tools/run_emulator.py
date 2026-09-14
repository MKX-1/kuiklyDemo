"""一键把整套跑起来：确保后端在跑 -> 启动 AVD -> 等开机 -> 装 APK -> 打开页面 -> 截图。

用法（在本文件所在目录的上一级，也就是工程根目录执行）:
    python tools/run_emulator.py

设计要点（踩过的坑都写在这）:
  1. 模拟器和后端都用 DETACHED_PROCESS 启动 —— 否则调用方一退出，它们会被跟着回收，
     表现为"刚跑起来就没了、adb devices 变空"。
  2. 只清掉 ANDROID_SDK_HOME（历史遗留变量，会让 adb/avdmanager 解析路径时直接失败）；
     ANDROID_HOME / ANDROID_SDK_ROOT / ANDROID_USER_HOME / ANDROID_AVD_HOME 反过来要显式设成 E 盘的值 ——
     AVD 已经整体搬到 E 盘，清掉 AVD_HOME 就会找不到实例。
  3. 命令行不能走 .bat（本机策略禁用 cmd.exe），所以直接调 sdkmanager/avdmanager 的 Java 主类。
  4. 本机 Android 工具链已全部迁到 E 盘（SDK / JDK / AVD / Gradle 缓存），C 盘不再增长。
"""
import os
import subprocess
import sys
import time
import urllib.request

SDK = r"E:\Android\Sdk"
ADB = os.path.join(SDK, "platform-tools", "adb.exe")
EMULATOR = os.path.join(SDK, "emulator", "emulator.exe")
JDK = r"E:\Android\Jdk17\jdk-17.0.16+8"
USER_HOME = r"E:\Android\UserHome"
AVD_HOME = os.path.join(USER_HOME, "avd")

PROJECT = r"E:\ai-stock-demo"
BACKEND = os.path.join(PROJECT, "backend")
AVD = "aistock"
APK = os.path.join(PROJECT, "androidApp", "build", "outputs", "apk", "debug", "androidApp-debug.apk")
SHOT = os.path.join(PROJECT, "docs", "screenshot-emulator.png")
PKG_ACTIVITY = "com.example.aistock/.KuiklyRenderActivity"
HEALTH = "http://127.0.0.1:8080/health"

ENV = dict(os.environ)
ENV.pop("ANDROID_SDK_HOME", None)
ENV["ANDROID_HOME"] = SDK
ENV["ANDROID_SDK_ROOT"] = SDK
ENV["ANDROID_USER_HOME"] = USER_HOME
ENV["ANDROID_AVD_HOME"] = AVD_HOME
ENV["JAVA_HOME"] = JDK

DETACH = (subprocess.DETACHED_PROCESS | subprocess.CREATE_NEW_PROCESS_GROUP) if os.name == "nt" else 0


def log(msg):
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


def run(cmd, timeout=120, **kw):
    return subprocess.run(cmd, capture_output=True, text=True, timeout=timeout,
                          env=ENV, encoding="utf-8", errors="replace", **kw)


def adb(*args, timeout=120):
    return run([ADB] + list(args), timeout=timeout)


def online_emulator():
    """返回当前已连接的模拟器序列号（例如 emulator-5554），没有则 None。"""
    for line in (adb("devices").stdout or "").splitlines():
        if line.startswith("emulator-"):
            return line.split()[0]
    return None


def is_booted(serial):
    out = adb("shell", "getprop", "sys.boot_completed", timeout=30).stdout or ""
    return out.strip() == "1"


def emulator_process_running():
    """判断是否已有模拟器进程（哪怕还没注册到 adb）。

    有进程却查不到设备时必须等它，否则会再开一个实例 -> AVD 被占用 -> 启动失败。
    """
    try:
        r = subprocess.run(["tasklist", "/FI", "IMAGENAME eq qemu-system-x86_64.exe"],
                           capture_output=True, text=True, encoding="gbk", errors="replace")
        return "qemu-system" in (r.stdout or "").lower()
    except Exception:
        return False


def wait_boot(serial, timeout=300):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if is_booted(serial):
            return True
        time.sleep(3)
    return False


def upstream_ok() -> bool:
    """后端探活。注意绕开系统代理，否则本机 127.0.0.1 会被代理拦。"""
    try:
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
        with opener.open(HEALTH, timeout=4) as r:
            return r.status == 200
    except Exception:
        return False


def ensure_backend():
    if upstream_ok():
        log("后端已在运行")
        return True
    log("后端未运行，正在拉起 …")
    logfile = open(os.path.join(os.environ.get("TEMP", "."), "aistock-backend.log"), "w",
                   encoding="utf-8", errors="replace")
    subprocess.Popen(
        [os.path.join(JDK, "bin", "java.exe"),
         "-classpath", os.path.join(BACKEND, "gradle", "wrapper", "gradle-wrapper.jar"),
         "org.gradle.wrapper.GradleWrapperMain", "run", "--no-daemon", "--console=plain"],
        cwd=BACKEND, env=ENV, stdout=logfile, stderr=subprocess.STDOUT,
        stdin=subprocess.DEVNULL, creationflags=DETACH,
    )
    for _ in range(75):
        if upstream_ok():
            log("后端就绪：http://127.0.0.1:8080")
            return True
        time.sleep(2)
    log("后端启动超时（不影响看界面：App 会自动降级到直连腾讯）")
    return False


def main():
    if not os.path.exists(APK):
        log(f"找不到 APK：{APK}，先执行 gradlew.bat assembleDebug")
        return 1

    chk = run([EMULATOR, "-accel-check"], timeout=60)
    accel = (chk.stdout or "") + (chk.stderr or "")
    if "is installed and usable" not in accel and "WHPX" not in accel:
        log("没有可用的硬件加速，模拟器无法正常开机：")
        log(accel.strip())
        return 2
    log("硬件加速可用")

    ensure_backend()

    existing = online_emulator()
    if not existing and emulator_process_running():
        log("检测到模拟器进程正在启动，等它注册到 adb（不再开新实例）…")
        for _ in range(60):
            existing = online_emulator()
            if existing:
                break
            time.sleep(3)

    if existing:
        log(f"已有模拟器在线（{existing}），直接复用，不再启动新实例")
        serial = existing
        if not wait_boot(serial):
            log("已有实例未开机完成")
            return 3
    else:
        log(f"启动模拟器 {AVD}（脱离进程，脚本退出后仍会保留）…")
        subprocess.Popen(
            [EMULATOR, "-avd", AVD, "-no-snapshot", "-no-boot-anim",
             "-gpu", "swiftshader_indirect", "-netdelay", "none", "-netspeed", "full"],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, stdin=subprocess.DEVNULL,
            env=ENV, creationflags=DETACH,
        )
        log("等待开机完成 …")
        serial, deadline = None, time.time() + 480
        while time.time() < deadline:
            serial = online_emulator()
            if serial and is_booted(serial):
                log(f"开机完成：{serial}")
                break
            time.sleep(5)
        else:
            log("等超时了：模拟器未能开机")
            return 3

    r = adb("reverse", "tcp:8080", "tcp:8080")
    log("adb reverse 8080 -> " + ("成功" if r.returncode == 0 else "失败"))

    log("安装 APK …")
    r = adb("install", "-r", "-t", APK, timeout=300)
    res = (r.stdout or "") + (r.stderr or "")
    log("Success" if "Success" in res else res.strip())
    if "Success" not in res:
        return 4

    log("启动 App …")
    adb("shell", "am", "start", "-n", PKG_ACTIVITY)
    time.sleep(18)

    os.makedirs(os.path.dirname(SHOT), exist_ok=True)
    with open(SHOT, "wb") as f:
        subprocess.run([ADB, "exec-out", "screencap", "-p"], stdout=f,
                       stderr=subprocess.DEVNULL, env=ENV, timeout=120)
    log(f"截图已保存：{SHOT}")

    log("---- App 日志（含异常关键字）----")
    lg = adb("logcat", "-d", "-t", "400").stdout or ""
    keys = ("aistock", "Kuikly", "ContextCodeHandler", "AndroidRuntime", "FATAL")
    for line in [l for l in lg.splitlines() if any(k in l for k in keys)][-25:]:
        print("   " + line)
    log("完成。模拟器窗口保持打开，可直接在上面操作。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
