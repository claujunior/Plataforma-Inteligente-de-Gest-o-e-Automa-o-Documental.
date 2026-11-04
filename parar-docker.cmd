@echo off
setlocal
title Parar Redis e PostgreSQL no Docker
set "servicos_docker_exe=docker"
where docker >nul 2>&1
if errorlevel 1 (
    if exist "%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe" (
        set "servicos_docker_exe=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe"
    ) else if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" (
        set "servicos_docker_exe=%ProgramFiles%\Docker\Docker\resources\bin\docker.exe"
    ) else (
        echo O Docker Desktop nao foi encontrado.
        pause
        exit /b 1
    )
)
"%servicos_docker_exe%" compose --project-directory "%~dp0." -f "%~dp0compose.yaml" stop redis postgres
set "servicos_resultado=%errorlevel%"
echo.
if "%servicos_resultado%"=="0" echo Redis e PostgreSQL parados. Os volumes foram preservados.
pause
exit /b %servicos_resultado%
