@echo off
rem AI 股票 Demo 后端启动脚本（Ktor，端口 8080）
rem 已内置百炼 API key 启用 LLM 分析（此文件不入 git）
cd /d E:\ai-stock-demo\backend
set DASHSCOPE_API_KEY=sk-f0d4dd87aff94e92b48b85a419d6ac97
set LLM_MODEL=qwen-turbo
"E:\Android\Jdk17\jdk-17.0.16+8\bin\java.exe" -cp "E:\ai-stock-demo\backend\build\install\aistock-backend\lib\*" com.example.aistock.backend.ApplicationKt > E:\ai-stock-demo\.logs\backend-run.txt 2>&1
