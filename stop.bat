@echo off
CHCP 65001 > nul
title LingoFlow 一键停止脚本

echo ======================================================
echo           LingoFlow 服务停止器
echo ======================================================
echo.

echo [1/3] 正在关闭前端服务 (Port: 5173)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :5173') do taskkill /f /pid %%a >nul 2>&1

echo [2/3] 正在关闭后端服务 (Port: 8080)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080') do taskkill /f /pid %%a >nul 2>&1

echo [3/3] 正在关闭 AI 服务 (Port: 5000)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :5000') do taskkill /f /pid %%a >nul 2>&1

echo.
echo ======================================================
echo 所有相关端口 (5173, 8080, 5000) 的进程已尝试终止。
echo ======================================================
timeout /t 3
exit
