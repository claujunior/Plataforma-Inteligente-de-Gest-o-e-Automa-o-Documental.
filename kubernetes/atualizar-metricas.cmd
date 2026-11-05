@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0atualizar-metricas.ps1"
set "resultado=%ERRORLEVEL%"
if not "%resultado%"=="0" echo ERRO: a atualizacao foi interrompida.
echo.
pause
exit /b %resultado%
