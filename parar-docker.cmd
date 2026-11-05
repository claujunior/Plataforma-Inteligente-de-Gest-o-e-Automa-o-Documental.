@echo off
setlocal
title Parar projeto Docker Compose
set "servicos_docker_exe=docker"
where docker >nul 2>&1
if errorlevel 1 (
    if exist "%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe" (
        set "servicos_docker_exe=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe"
    ) else if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" (
        set "servicos_docker_exe=%ProgramFiles%\Docker\Docker\resources\bin\docker.exe"
    ) else (
        echo Docker nao encontrado. Instale ou abra o Docker Desktop.
        pause
        exit /b 1
    )
)
"%servicos_docker_exe%" compose --project-directory "%~dp0." -f "%~dp0compose.yaml" stop
set "servicos_resultado=%errorlevel%"
echo.
if "%servicos_resultado%"=="0" echo Todos os servicos do Compose foram parados. Os volumes foram preservados.
pause
exit /b %servicos_resultado%
