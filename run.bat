@echo off
CHCP 65001 > nul
title LingoFlow 一键启动脚本

echo ======================================================
echo           LingoFlow 全栈启动器 (Premium Version)
echo ======================================================
echo.
echo [1/3] 正在启动 AI 服务... (Port: 5000)
start "LingoFlow-AI-Service" cmd /k "cd ai-service && python app.py"

echo [2/3] 正在启动 Backend 后端服务... (Port: 8080)
start "LingoFlow-Backend" cmd /k "cd backend && mvn spring-boot:run"

echo [3/3] 正在启动 Frontend 前端页面... (Port: 5173)
start "LingoFlow-Frontend" cmd /k "cd frontend && npm run dev"

echo.
echo ======================================================
echo 启动指令已发出，请在弹出的新窗口中查看运行状态：
echo - 前端地址: http://localhost:5173
echo - 后端 API: http://localhost:8080
echo - AI 服务 : http://localhost:5000
echo ======================================================
pause
