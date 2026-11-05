$ErrorActionPreference = 'Stop'
# Mantem o comando anterior funcionando com a reconstrucao completa.
& (Join-Path $PSScriptRoot 'reconstruir-tudo.ps1')
