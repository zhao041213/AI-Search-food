[CmdletBinding()]
param(
    [string]$NginxHome = $env:NGINX_HOME
)

$frontendRoot = Split-Path -Parent $PSScriptRoot
$runtimeConfigPath = Join-Path $frontendRoot 'nginx/runtime-local.conf'

function Resolve-NginxInstallation([string]$configuredHome) {
    $candidateHomes = @($configuredHome, $env:NGINX_HOME, 'C:\nginx') |
        Where-Object { $_ -and (Test-Path -LiteralPath $_) } |
        Select-Object -Unique

    foreach ($candidateHome in $candidateHomes) {
        $candidateExecutable = Join-Path $candidateHome 'nginx.exe'
        if (Test-Path -LiteralPath $candidateExecutable) {
            return [pscustomobject]@{
                Home = (Resolve-Path -LiteralPath $candidateHome).Path
                Executable = (Resolve-Path -LiteralPath $candidateExecutable).Path
            }
        }
    }

    $command = Get-Command nginx.exe -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return [pscustomobject]@{
            Home = Split-Path -Parent $command.Source
            Executable = $command.Source
        }
    }

    $wingetPackages = Join-Path $env:LOCALAPPDATA 'Microsoft\WinGet\Packages'
    $wingetExecutable = Get-ChildItem -LiteralPath $wingetPackages -Filter nginx.exe -Recurse `
        -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -ne $wingetExecutable) {
        return [pscustomobject]@{
            Home = $wingetExecutable.DirectoryName
            Executable = $wingetExecutable.FullName
        }
    }

    throw 'Nginx was not found. Set NGINX_HOME and try again.'
}

if (-not (Test-Path -LiteralPath $runtimeConfigPath)) {
    throw 'The project Nginx runtime configuration was not found.'
}

$nginx = Resolve-NginxInstallation $NginxHome
& $nginx.Executable -p $nginx.Home -c $runtimeConfigPath -s quit
if ($LASTEXITCODE -ne 0) {
    throw 'Nginx failed to stop.'
}

Write-Output 'Nginx stopped.'
