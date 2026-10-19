$ErrorActionPreference = "Stop"

$ProjectDir = "C:\Data Collector\Data Proccesor\json-data-collector"
$JarPath = Join-Path $ProjectDir "target\demo-0.0.1-SNAPSHOT.jar"
$LogDir = Join-Path $ProjectDir "logs"

$AppUrl = "http://localhost:8080"
$RunUrl = "$AppUrl/run"

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$StdoutLog = Join-Path $LogDir "scheduled-console.log"
$StderrLog = Join-Path $LogDir "scheduled-errors.log"

if (!(Test-Path $JarPath)) {
    throw "JAR not found: $JarPath. Build the project first."
}

# Confirm required environment variables are available
if ([string]::IsNullOrWhiteSpace($env:EXTERNAL_API_URL)) {
    throw "EXTERNAL_API_URL is not configured."
}

if ([string]::IsNullOrWhiteSpace($env:EXTERNAL_API_TOKEN)) {
    throw "EXTERNAL_API_TOKEN is not configured."
}

$JavaProcess = $null

try {
    # Avoid accidentally starting a second server on port 8080
    $ExistingServer = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue

    if ($ExistingServer) {
        throw "Port 8080 is already in use. Stop the existing application before running this task."
    }

    Write-Output "Starting JSON collector..."

    $JavaProcess = Start-Process `
        -FilePath "java" `
        -ArgumentList @("-jar", "`"$JarPath`"") `
        -WorkingDirectory $ProjectDir `
        -RedirectStandardOutput $StdoutLog `
        -RedirectStandardError $StderrLog `
        -PassThru `
        -WindowStyle Hidden

    # Wait up to 90 seconds for Spring Boot
    $Ready = $false

    for ($i = 0; $i -lt 90; $i++) {
        if ($JavaProcess.HasExited) {
            throw "Java application exited before becoming ready. Check logs."
        }

        try {
            $Response = Invoke-WebRequest `
                -Uri "$AppUrl/actuator/health" `
                -TimeoutSec 2 `
                -UseBasicParsing

            if ($Response.StatusCode -eq 200) {
                $Ready = $true
                break
            }
        }
        catch {
            # Not ready yet; retry
        }

        Start-Sleep -Seconds 1
    }

    # If Actuator health endpoint is not configured, check the root endpoint
    if (-not $Ready) {
        for ($i = 0; $i -lt 30; $i++) {
            if ($JavaProcess.HasExited) {
                throw "Java application exited during startup. Check logs."
            }

            try {
                Invoke-WebRequest -Uri $RunUrl -Method Get -TimeoutSec 5 -UseBasicParsing | Out-Null
                $Ready = $true
                break
            }
            catch {
                # This may call /run if the app is ready but the endpoint returned an error.
                # See note below before using this fallback.
            }

            Start-Sleep -Seconds 1
        }
    }

    if (-not $Ready) {
        throw "Application did not become ready within the startup timeout."
    }

    Write-Output "Application ready. Triggering collector job..."

    $Result = Invoke-WebRequest `
        -Uri $RunUrl `
        -Method Get `
        -TimeoutSec 600 `
        -UseBasicParsing

    Write-Output "Collector endpoint returned HTTP $($Result.StatusCode)"
    Write-Output $Result.Content
}
finally {
    if ($JavaProcess -and -not $JavaProcess.HasExited) {
        Write-Output "Stopping Java application..."
        Stop-Process -Id $JavaProcess.Id -Force
    }
}