@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Instalar.ps1"
if errorlevel 1 (
    echo.
    echo La instalacion no pudo completarse. Revise el mensaje anterior.
    pause
    exit /b 1
)
exit /b 0
