@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0reconstruir-tudo.ps1"
set "resultado=%ERRORLEVEL%"
if not "%resultado%"=="0" echo ERRO: a reconstrucao foi interrompida.
echo.
pause
exit /b %resultado%
