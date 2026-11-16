@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ======================================================
echo   Universal API Studio (OSS) - 一键推送到 GitHub
echo   Author: @Azumaserne
echo ======================================================
echo.
echo 请先在 GitHub 创建一个空仓库（例如 Universal-API-Studio），不要勾选初始化 README。
set /p REPO_URL=请输入你的 GitHub 仓库地址 (如 https://github.com/Azumaserne/Universal-API-Studio.git): 

if "%REPO_URL%"=="" (
  echo [错误] 仓库地址不能为空！
  pause
  exit /b 1
)

git remote remove origin >nul 2>&1
git remote add origin "%REPO_URL%"
git branch -M main
echo.
echo 正在推送 main 分支与 v1.0.0 标签（如果弹出登录窗口请按提示完成 GitHub 登录）...
git push -u origin main
git push origin v1.0.0
echo.
echo 推送完成！
echo 如果你打上了 v1.0.0 标签，GitHub Actions 将自动编译并创建 Release，
echo 你也可以直接把 releases\UniversalApiStudio-OSS-v1.0.0.apk 手动上传到 Releases 页面。
pause
