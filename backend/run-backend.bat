@echo off
rem AI 股票 Demo 后端启动脚本（Ktor，端口 8080）
rem 用法：双击运行，或由任务计划程序调用
cd /d E:\ai-stock-demo\backend
"E:\Android\Jdk17\jdk-17.0.16+8\bin\java.exe" -cp "E:\ai-stock-demo\backend\build\install\aistock-backend\lib\*" com.example.aistock.backend.ApplicationKt > E:\ai-stock-demo\.logs\backend-run.txt 2>&1
