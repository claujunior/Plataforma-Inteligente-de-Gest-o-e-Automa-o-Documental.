@echo off
setlocal
title Iniciar Redis no Docker
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
"%redis_docker_exe%" info >nul 2>&1
if errorlevel 1 (
    echo Abra o Docker Desktop e aguarde o mecanismo Docker iniciar.
    pause
    exit /b 1
)
echo Iniciando somente Redis e removendo os antigos conteineres Java deste Compose...
"%redis_docker_exe%" compose --project-directory "%~dp0." -f "%~dp0compose.yaml" up -d --remove-orphans --wait --wait-timeout 60 redis
set "redis_resultado=%errorlevel%"
if not "%redis_resultado%"=="0" (
    echo.
    echo Falha ao iniciar Redis. Confira os logs:
    "%redis_docker_exe%" compose --project-directory "%~dp0." -f "%~dp0compose.yaml" logs --tail 30 redis
) else (
    echo.
    echo Redis pronto em localhost:6379.
    echo Inicie Config Server, Eureka, Alpha e Gateway pela IDE ou pelo iniciar-tudo.cmd.
)
echo.
pause
exit /b %redis_resultado%
