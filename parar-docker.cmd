@echo off
setlocal
title Parar Redis no Docker
set "redis_docker_exe=docker"
where docker >nul 2>&1
if errorlevel 1 (
    if exist "%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe" (
        set "redis_docker_exe=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe"
    ) else if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" (
        set "redis_docker_exe=%ProgramFiles%\Docker\Docker\resources\bin\docker.exe"
    ) else (
        echo O Docker Desktop nao foi encontrado.
        pause
        exit /b 1
    )
)
"%redis_docker_exe%" compose --project-directory "%~dp0." -f "%~dp0compose.yaml" stop redis
set "redis_resultado=%errorlevel%"
echo.
if "%redis_resultado%"=="0" echo Redis parado. O conteiner e seu volume foram preservados.
pause
exit /b %redis_resultado%
