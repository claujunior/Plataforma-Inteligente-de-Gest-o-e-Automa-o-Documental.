@echo off
setlocal
title Iniciar Redis e PostgreSQL no Docker
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
"%servicos_docker_exe%" info >nul 2>&1
if errorlevel 1 (
    echo Abra o Docker Desktop e aguarde o mecanismo Docker iniciar.
    pause
    exit /b 1
)
echo Iniciando Redis e PostgreSQL...
"%servicos_docker_exe%" compose --project-directory "%~dp0." -f "%~dp0compose.yaml" up -d --wait --wait-timeout 120 redis postgres
set "servicos_resultado=%errorlevel%"
if not "%servicos_resultado%"=="0" (
    echo.
    echo Falha ao iniciar Redis e PostgreSQL. Confira os logs:
    "%servicos_docker_exe%" compose --project-directory "%~dp0." -f "%~dp0compose.yaml" logs --tail 30 redis postgres
) else (
    echo.
    echo Redis pronto em localhost:6379.
    echo PostgreSQL pronto em 127.0.0.1:5432. Banco: distribuidos3. Usuario: postgres.
    echo Inicie Config Server, Eureka, Alpha e Gateway pela IDE ou pelo iniciar-tudo.cmd.
)
echo.
pause
exit /b %servicos_resultado%
