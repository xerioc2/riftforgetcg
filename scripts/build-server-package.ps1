#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Write-ProgressMessage([string]$Message) {
  Write-Host $Message -ForegroundColor Cyan
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$serverDir = Join-Path $repoRoot 'server'
$serverTarget = Join-Path $serverDir 'target'
$jreOut = Join-Path $serverTarget 'server-jre'
$hostDir = Join-Path $repoRoot 'target\riftforge-host'
$zipPath = Join-Path $repoRoot 'target\riftforge-server-host-windows.zip'

if (-not $env:JAVA_HOME) {
  throw 'JAVA_HOME must point to a full Java 21 JDK.'
}

$jlink = Join-Path $env:JAVA_HOME 'bin\jlink.exe'
$jmods = Join-Path $env:JAVA_HOME 'jmods'
if (-not (Test-Path -LiteralPath $jlink)) { throw "jlink not found at $jlink" }
if (-not (Test-Path -LiteralPath $jmods)) { throw "JDK modules not found at $jmods" }

Write-ProgressMessage 'Building Spring Boot server JAR...'
Push-Location $serverDir
try {
  mvn -DskipTests package
  if ($LASTEXITCODE -ne 0) { throw "Maven build failed with exit code $LASTEXITCODE." }
} finally {
  Pop-Location
}

$serverJar = Get-ChildItem -LiteralPath $serverTarget -Filter 'riftforge-server-*.jar' |
  Where-Object { $_.Name -notlike '*.original' } |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1
if (-not $serverJar) { throw 'Built Spring Boot server JAR was not found.' }

Write-ProgressMessage 'Creating minimal Java runtime with jlink...'
if (Test-Path -LiteralPath $jreOut) { Remove-Item -LiteralPath $jreOut -Recurse -Force }
& $jlink `
  --module-path $jmods `
  --add-modules java.base,java.logging,java.xml,java.naming,java.management,java.instrument,jdk.unsupported,java.sql,jdk.crypto.ec,java.security.jgss,java.net.http,jdk.localedata,java.desktop,jdk.zipfs `
  --output $jreOut `
  --strip-debug --no-header-files --no-man-pages --compress=2
if ($LASTEXITCODE -ne 0) { throw "jlink failed with exit code $LASTEXITCODE." }

Write-ProgressMessage 'Assembling standalone host package...'
if (Test-Path -LiteralPath $hostDir) { Remove-Item -LiteralPath $hostDir -Recurse -Force }
New-Item -ItemType Directory -Path $hostDir -Force | Out-Null
Copy-Item -LiteralPath $jreOut -Destination (Join-Path $hostDir 'jre') -Recurse
Copy-Item -LiteralPath $serverJar.FullName -Destination (Join-Path $hostDir 'riftforge-server.jar')

@'
@echo off
echo Starting RiftForge server on port 8080...
jre\bin\java.exe -jar riftforge-server.jar
pause
'@ | Set-Content -LiteralPath (Join-Path $hostDir 'start-server.bat') -Encoding ascii

@'
Write-Host "Starting RiftForge server on port 8080..." -ForegroundColor Cyan
& (Join-Path $PSScriptRoot 'jre\bin\java.exe') -jar (Join-Path $PSScriptRoot 'riftforge-server.jar')
'@ | Set-Content -LiteralPath (Join-Path $hostDir 'start-server.ps1') -Encoding utf8

@'
RiftForge Host Server
=====================

1. Run start-server.bat (or start-server.ps1 in PowerShell)
   The server starts on http://localhost:8080

2. Install ngrok (free): https://ngrok.com/download

3. In a second terminal run:
     ngrok http 8080
   Copy the https://xxx.ngrok-free.app URL shown.

4. Share that URL with players.
   Players open it in any modern browser - no install required.

5. To stop the server, close the first terminal window.

Note: The ngrok URL changes each session on the free plan.
To get a permanent URL, create a free ngrok account and use
a static domain.
'@ | Set-Content -LiteralPath (Join-Path $hostDir 'HOSTING.txt') -Encoding utf8

Write-ProgressMessage 'Compressing host package...'
if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath -Force }
$archiveError = $null
for ($attempt = 1; $attempt -le 3; $attempt++) {
  try {
    if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath -Force }
    Compress-Archive -LiteralPath $hostDir -DestinationPath $zipPath -CompressionLevel Optimal -ErrorAction Stop
    $archiveError = $null
    break
  } catch {
    $archiveError = $_
    if ($attempt -lt 3) {
      Write-ProgressMessage "Archive attempt $attempt failed; retrying..."
      Start-Sleep -Seconds 2
    }
  }
}
if ($archiveError -or -not (Test-Path -LiteralPath $zipPath)) {
  throw "Unable to create host package archive: $archiveError"
}

$zip = Get-Item -LiteralPath $zipPath
$sizeMb = [Math]::Round($zip.Length / 1MB, 2)
Write-ProgressMessage "Created $($zip.FullName) ($sizeMb MB)"
