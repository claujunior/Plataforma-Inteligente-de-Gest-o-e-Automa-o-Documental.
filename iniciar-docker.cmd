@echo off
setlocal
title Iniciar projeto com Docker Compose
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
"%servicos_docker_exe%" info >nul 2>&1
if errorlevel 1 (
    echo Abra o Docker Desktop e aguarde o mecanismo Docker iniciar.
    pause
    exit /b 1
)
"%servicos_docker_exe%" compose --project-directory "%~dp0." -f "%~dp0compose.yaml" config --quiet
if errorlevel 1 goto falha

echo Construindo as quatro imagens Java...
"%servicos_docker_exe%" compose --project-directory "%~dp0." -f "%~dp0compose.yaml" build config-server discovery-1 service-alpha api-gateway
if errorlevel 1 goto falha

echo Iniciando os servicos na ordem de dependencias e aguardando a saude...
"%servicos_docker_exe%" compose --project-directory "%~dp0." -f "%~dp0compose.yaml" up -d --no-build --wait --wait-timeout 600
if errorlevel 1 goto falha

echo.
echo Gateway: http://127.0.0.1:30080/service-alpha/teste
echo Eureka: http://127.0.0.1:30761
echo Prometheus: http://127.0.0.1:30900/targets
echo Os dados do Compose foram preservados. Os dados do Kubernetes sao separados.
pause
exit /b 0

:falha
echo.
echo Falha ao construir ou iniciar o projeto. Etapas dependentes nao serao iniciadas.
echo Consulte: docker compose ps -a
echo Consulte: docker compose logs --tail 80
echo Nenhum volume foi excluido.
pause
exit /b 1
