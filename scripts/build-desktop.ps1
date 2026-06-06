#!/usr/bin/env pwsh
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "Building native server (this takes 3-8 minutes)..."
Push-Location server
try {
  mvn -Pnative -DskipTests package
} finally {
  Pop-Location
}

$triple = (rustc -vV | Select-String "host:").ToString().Split(":")[1].Trim()
New-Item -ItemType Directory -Force src-tauri/binaries | Out-Null
Copy-Item "server/target/riftforge-server.exe" `
          "src-tauri/binaries/riftforge-server-$triple.exe" -Force
Write-Host "Sidecar placed at src-tauri/binaries/riftforge-server-$triple.exe"

npm run tauri:build
Write-Host "Done. Installer is in src-tauri/target/release/bundle/"
