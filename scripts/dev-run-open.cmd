@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
set "PS_SCRIPT=%SCRIPT_DIR%dev-run.ps1"

if not exist "%PS_SCRIPT%" (
  echo [ERROR] Missing script: "%PS_SCRIPT%"
  pause
  exit /b 1
)

powershell.exe -NoLogo -NoExit -ExecutionPolicy Bypass -File "%PS_SCRIPT%" -KeepOpen %*
set "RC=%ERRORLEVEL%"

echo.
echo [dev-run-open] exit code: %RC%
pause
exit /b %RC%
