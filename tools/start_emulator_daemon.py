"""常驻守护：拉起后端 + 模拟器，然后一直挂着不退出。

为什么要它：本机沙箱会在后台任务结束时回收它的子进程，
所以模拟器/后端必须挂在一个"不会结束"的任务里才能活着。
用 run_emulator.py 跑流程（它会自动复用已在线的设备），本脚本只负责保活。

用法:
    python tools/start_emulator_daemon.py        # 前台运行，Ctrl+C 结束
后台任务里跑它就等于"让模拟器一直开着"。
"""
import os
import subprocess
import sys
import time
import urllib.request

SDK = r"E:\Android\Sdk"
JDK = r"E:\Android\Jdk17\jdk-17.0.16+8"
USER_HOME = r"E:\Android\UserHome"
AVD_HOME = os.path.join(USER_HOME, "avd")
PROJECT = r"E:\ai-stock-demo"
BACKEND = os.path.join(PROJECT, "backend")
AVD = "aistock"
LOG = os.path.join(os.environ.get("TEMP", "."), "aistock-stack.log")

ENV = dict(os.environ)
ENV.pop("ANDROID_SDK_HOME", None)
ENV["ANDROID_HOME"] = SDK
ENV["ANDROID_SDK_ROOT"] = SDK
ENV["ANDROID_USER_HOME"] = USER_HOME
ENV["ANDROID_AVD_HOME"] = AVD_HOME
ENV["GRADLE_USER_HOME"] = r"E:\Android\gradle-home"
ENV["JAVA_HOME"] = JDK


def log(msg):
    line = f"[{time.strftime('%H:%M:%S')}] {msg}"
    print(line, flush=True)
    with open(LOG, "a", encoding="utf-8", errors="replace") as f:
        f.write(line + "\n")


def backend_ok():
    try:
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
        with opener.open("http://127.0.0.1:8080/health", timeout=4) as r:
            return r.status == 200
    except Exception:
        return False


def main():
    open(LOG, "w", encoding="utf-8").close()

    # ---- 后端 ----
    if backend_ok():
        log("后端已在运行，跳过")
        be = None
    else:
        log("拉起后端 …")
        be_log = open(os.path.join(os.environ.get("TEMP", "."), "aistock-backend.log"),
                      "w", encoding="utf-8", errors="replace")
        be = subprocess.Popen(
            [os.path.join(JDK, "bin", "java.exe"),
             "-classpath", os.path.join(BACKEND, "gradle", "wrapper", "gradle-wrapper.jar"),
             "org.gradle.wrapper.GradleWrapperMain", "run", "--no-daemon", "--console=plain"],
            cwd=BACKEND, env=ENV, stdout=be_log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL,
        )
        log(f"后端 pid={be.pid}")

    # ---- 模拟器 ----
    emu = subprocess.Popen(
        [os.path.join(SDK, "emulator", "emulator.exe"), "-avd", AVD,
         "-no-snapshot", "-no-boot-anim", "-gpu", "swiftshader_indirect",
         "-netdelay", "none", "-netspeed", "full"],
        env=ENV, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, stdin=subprocess.DEVNULL,
    )
    log(f"模拟器已启动 pid={emu.pid}；本进程将一直挂着保活")

    try:
        rc = emu.wait()
        log(f"模拟器退出 rc={rc}")
    except KeyboardInterrupt:
        log("收到中断，关闭模拟器")
        emu.terminate()
    finally:
        if be is not None:
            log("关闭后端")
            be.terminate()
    return 0


if __name__ == "__main__":
    sys.exit(main())
