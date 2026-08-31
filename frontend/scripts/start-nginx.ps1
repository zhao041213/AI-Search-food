[CmdletBinding()]
param(
    [string]$NginxHome = $env:NGINX_HOME,
    [ValidateRange(1024, 65535)]
    [int]$Port = 5173,
    [switch]$SkipBuild
)

$frontendRoot = Split-Path -Parent $PSScriptRoot
$templatePath = Join-Path $frontendRoot 'nginx/nginx.local.conf.template'
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

    throw 'Nginx was not found. Extract it to C:\nginx or set NGINX_HOME.'
}

if (-not $SkipBuild) {
    Push-Location $frontendRoot
    try {
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) {
            throw 'Frontend build failed. Nginx was not started.'
        }
    } finally {
        Pop-Location
    }
}

$nginx = Resolve-NginxInstallation $NginxHome
$existingListener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -First 1
if ($null -ne $existingListener) {
    throw "Port $Port is already in use by process $($existingListener.OwningProcess). Stop Vite or pass a different -Port value."
}

$distPath = (Resolve-Path -LiteralPath (Join-Path $frontendRoot 'dist')).Path.Replace('\', '/')
$nginxHomePath = $nginx.Home.Replace('\', '/')
$config = Get-Content -LiteralPath $templatePath -Raw
$config = $config.Replace('__FRONTEND_DIST__', $distPath)
$config = $config.Replace('__NGINX_HOME__', $nginxHomePath)
$config = $config.Replace('__LISTEN_PORT__', $Port.ToString())
[System.IO.File]::WriteAllText($runtimeConfigPath, $config, [System.Text.UTF8Encoding]::new($false))

& $nginx.Executable -t -p $nginx.Home -c $runtimeConfigPath
if ($LASTEXITCODE -ne 0) {
    throw 'Nginx configuration validation failed.'
}

Start-Process -FilePath $nginx.Executable `
    -ArgumentList @('-p', $nginx.Home, '-c', $runtimeConfigPath) `
    -WindowStyle Hidden
Start-Sleep -Milliseconds 300

$listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -First 1
if ($null -eq $listener) {
    throw 'Nginx failed to start.'
}

Write-Output "Nginx started: http://localhost:$Port"
