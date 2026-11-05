$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$tag = 'build-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$images = @(
    @{ Directory = 'config-server'; Repository = 'distribuidos3-config'; Manifest = 'config-server.yaml'; Expected = 1 },
    @{ Directory = 'discovery-server'; Repository = 'distribuidos3-discovery'; Manifest = 'discovery.yaml'; Expected = 3 },
    @{ Directory = 'service-alpha'; Repository = 'distribuidos3-alpha'; Manifest = 'service-alpha.yaml'; Expected = 1 },
    @{ Directory = 'ApiGateway'; Repository = 'distribuidos3-gateway'; Manifest = 'api-gateway.yaml'; Expected = 1 }
)

# Confere todos os arquivos antes de construir ou alterar os manifestos.
$updates = @{}
foreach ($item in $images) {
    $dockerfile = Join-Path (Join-Path $project $item.Directory) 'Dockerfile'
    if (-not (Test-Path -LiteralPath $dockerfile)) { throw "Dockerfile nao encontrado: $dockerfile" }
    $path = Join-Path $PSScriptRoot $item.Manifest
    $content = Get-Content -LiteralPath $path -Raw
    $pattern = '(?m)^([ \t]+image:[ \t]+)' + [regex]::Escape($item.Repository) + ':[A-Za-z0-9_.-]+(\r?)$'
    if ([regex]::Matches($content, $pattern).Count -ne $item.Expected) {
        throw "Quantidade inesperada de imagens em $($item.Manifest). Nenhum arquivo foi alterado."
    }
    $replacement = '${1}' + $item.Repository + ':' + $tag + '${2}'
    $updates[$path] = [regex]::Replace($content, $pattern, $replacement)
}

Write-Host "Nova tag: $tag"
foreach ($item in $images) {
    $image = $item.Repository + ':' + $tag
    Write-Host "Reconstruindo $image"
    & docker --context desktop-linux build --no-cache -t $image (Join-Path $project $item.Directory)
    if ($LASTEXITCODE -ne 0) {
        throw "Falha na construcao de $image. Os manifestos e os servicos em execucao foram preservados."
    }
}

# So atualiza as referencias depois de todas as quatro construcoes passarem.
foreach ($path in $updates.Keys) {
    [System.IO.File]::WriteAllText($path, $updates[$path], [System.Text.UTF8Encoding]::new($false))
}
Write-Host 'Imagens construidas. Atualizando o Kubernetes na ordem configurada.'
& (Join-Path $PSScriptRoot 'iniciar-kubernetes.ps1')
