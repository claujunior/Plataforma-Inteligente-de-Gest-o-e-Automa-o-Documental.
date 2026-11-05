$ErrorActionPreference = 'Stop'
$namespace = 'distribuidos3-lab'

function Invoke-Kubernetes {
    param([string[]]$Parameters)

    & kubectl --context docker-desktop @Parameters
    if ($LASTEXITCODE -ne 0) {
        throw "Falha no kubectl. A inicializacao foi interrompida; consulte os Pods e os logs."
    }
}

function Start-Etapa {
    param(
        [string]$Manifest,
        [string[]]$Resources
    )

    Write-Host ""
    Write-Host "Iniciando: $Manifest"
    Invoke-Kubernetes -Parameters @('apply', '-f', (Join-Path $PSScriptRoot $Manifest))
    foreach ($resource in $Resources) {
        Invoke-Kubernetes -Parameters @('-n', $namespace, 'rollout', 'status', $resource, '--timeout=300s')
    }
}

# Usa sempre o cluster local e aguarda cada etapa antes de iniciar a seguinte.
Invoke-Kubernetes -Parameters @('get', 'nodes', '--request-timeout=10s')
Invoke-Kubernetes -Parameters @('apply', '-f', (Join-Path $PSScriptRoot 'namespace.yaml'))

Start-Etapa -Manifest 'config-server.yaml' -Resources @('deployment/config-server')
Invoke-Kubernetes -Parameters @('apply', '-f', (Join-Path $PSScriptRoot 'config-client.yaml'))
# Os tres peers iniciam juntos para evitar espera circular entre eles.
Start-Etapa -Manifest 'discovery.yaml' -Resources @('deployment/discovery-1', 'deployment/discovery-2', 'deployment/discovery-3')
Start-Etapa -Manifest 'redis.yaml' -Resources @('deployment/redis')
Start-Etapa -Manifest 'postgres.yaml' -Resources @('statefulset/postgres')
Start-Etapa -Manifest 'service-alpha.yaml' -Resources @('deployment/service-alpha')
Start-Etapa -Manifest 'api-gateway.yaml' -Resources @('deployment/api-gateway')
# Gera o ConfigMap a partir do prometheus.yml versionado no projeto.
$prometheusFile = Join-Path $PSScriptRoot 'prometheus/prometheus.yml'
$prometheusConfig = Invoke-Kubernetes -Parameters @('-n', $namespace, 'create', 'configmap', 'prometheus-config', "--from-file=prometheus.yml=$prometheusFile", '--dry-run=client', '-o', 'yaml')
$prometheusConfig | & kubectl --context docker-desktop apply -f -
if ($LASTEXITCODE -ne 0) { throw 'Falha ao aplicar a configuracao do Prometheus.' }
Start-Etapa -Manifest 'prometheus.yaml' -Resources @('deployment/prometheus')


Invoke-Kubernetes -Parameters @('-n', $namespace, 'get', 'pods,services,pvc')
Write-Host ""
Write-Host "Servicos prontos. Gateway: http://127.0.0.1:30080"
Write-Host "Painel Eureka 1: http://127.0.0.1:30761"
Write-Host "Prometheus: http://127.0.0.1:30900/targets"
Write-Host "Teste: curl.exe http://127.0.0.1:30080/service-alpha/teste"
