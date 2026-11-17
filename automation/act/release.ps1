param(
    [Parameter(Mandatory)][string]$Tag,
    [Parameter(Mandatory)][string]$Commit,
    [switch]$CreateDraft,
    [switch]$LocalSource
)

$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$envPath = Join-Path $projectRoot '.env'
$pathLine = Get-Content -LiteralPath $envPath |
    Where-Object { $_ -match '^ANDROID_KEYSTORE_PATH=' } | Select-Object -First 1
if (-not $pathLine) { throw 'Set ANDROID_KEYSTORE_PATH in .env.' }
$keystorePath = ($pathLine -split '=', 2)[1].Trim().Trim('"', "'")
if (-not (Test-Path -LiteralPath $keystorePath -PathType Leaf)) {
    throw 'ANDROID_KEYSTORE_PATH does not point to a keystore file.'
}

Push-Location $projectRoot
try {
    $actArguments = @(
        'workflow_dispatch', '-W', 'automation/act/release.yml',
        '--input', "tag=$Tag", '--input', "commit=$Commit",
        '--secret-file', $envPath,
        '-P', 'ubuntu-22.04=catthehacker/ubuntu:act-22.04',
        '--artifact-server-path', '.act-artifacts',
        '--container-options', "--mount type=bind,source=$keystorePath,target=/run/glider-signing.keystore,readonly"
    )
    if ($CreateDraft) { $actArguments += @('--input', 'create_draft=true') }
    if ($LocalSource) { $actArguments += @('--input', 'local_source=true') }
    & act @actArguments
    if ($LASTEXITCODE -ne 0) { throw "act exited with code $LASTEXITCODE" }
} finally {
    Pop-Location
}
