$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$directory = Join-Path $project 'local-deps'
$manifest = Get-Content -LiteralPath (Join-Path $directory 'compile-apis.json') -Raw | ConvertFrom-Json
foreach ($entry in $manifest) {
    $path = Join-Path $directory $entry.filename
    if (-not (Test-Path -LiteralPath $path)) {
        Invoke-WebRequest -Uri $entry.url -OutFile $path -Headers @{ 'User-Agent' = 'HBM-building-port-build/1.0' }
    }
    $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA512).Hash.ToLowerInvariant()
    if ($actual -ne $entry.sha512) { throw "SHA512 mismatch: $($entry.filename)" }
    Write-Output "$($entry.project) $($entry.version): SHA512 verified"
}
