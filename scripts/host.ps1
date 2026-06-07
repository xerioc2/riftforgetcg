# host.ps1 — Build the frontend and serve everything through Spring Boot.
# Share your ngrok URL and friends can play in a browser — no install needed.
#
# Prerequisites: Node 20+, Java 21, Maven, ngrok (https://ngrok.com/download)
#
# Usage:
#   .\scripts\host.ps1
# Then in a second terminal:
#   ngrok http 8080
# Share the https://xxx.ngrok-free.app URL with your players.

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

Write-Host "==> Building frontend..." -ForegroundColor Cyan
Push-Location $root
npm run build
if ($LASTEXITCODE -ne 0) { throw "Frontend build failed" }
Pop-Location

$static = Join-Path $root "server\src\main\resources\static"
Write-Host "==> Copying dist → $static" -ForegroundColor Cyan
if (Test-Path $static) { Remove-Item $static -Recurse -Force }
New-Item -ItemType Directory -Force $static | Out-Null
Copy-Item (Join-Path $root "dist\*") $static -Recurse

Write-Host "==> Starting Spring Boot (port 8080)..." -ForegroundColor Cyan
Write-Host "    Open a second terminal and run:  ngrok http 8080" -ForegroundColor Yellow
Write-Host "    Share the https://xxx.ngrok-free.app URL with players." -ForegroundColor Yellow
Push-Location (Join-Path $root "server")
$env:CORS_ORIGINS = "*"
mvn spring-boot:run
Pop-Location
