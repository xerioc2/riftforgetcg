#!/usr/bin/env pwsh
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not $env:VITE_BUILD_DATE) {
  $env:VITE_BUILD_DATE = (Get-Date).ToUniversalTime().ToString("yyyy-MM-dd")
}
Write-Host "Using frontend build date $env:VITE_BUILD_DATE"

Write-Host "Building RiftForge desktop package. Tauri will rebuild and verify the bundled server before packaging..."
npm run build:desktop
if ($LASTEXITCODE -ne 0) { throw "Tauri build failed with exit code $LASTEXITCODE." }
Write-Host "Done. Installer is in src-tauri/target/release/bundle/"
