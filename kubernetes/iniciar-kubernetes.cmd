@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar-kubernetes.ps1"
set "resultado=%ERRORLEVEL%"
if not "%resultado%"=="0" echo ERRO: a inicializacao foi interrompida.
echo.
pause
exit /b %resultado%
