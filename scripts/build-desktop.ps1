#!/usr/bin/env pwsh
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "Building server JAR..."
Push-Location server
try {
  mvn -DskipTests package
  $mavenExit = $LASTEXITCODE
} finally {
  Pop-Location
}
if ($mavenExit -ne 0) { throw "Maven server build failed with exit code $mavenExit." }

Write-Host "Creating stripped JRE with jlink..."
$jreOut = "server/target/server-jre"
if (Test-Path $jreOut) { Remove-Item -Recurse -Force $jreOut }
& "$env:JAVA_HOME\bin\jlink.exe" `
  --add-modules java.base,java.logging,java.xml,java.naming,java.management,java.instrument,jdk.unsupported,java.sql,jdk.crypto.ec,java.security.jgss,java.net.http,jdk.localedata,java.desktop,jdk.zipfs `
  --output $jreOut `
  --strip-debug --no-man-pages --no-header-files --compress=2
if ($LASTEXITCODE -ne 0) { throw "jlink failed with exit code $LASTEXITCODE." }

New-Item -ItemType Directory -Force src-tauri/binaries | Out-Null
if (Test-Path src-tauri/binaries/jre) { Remove-Item -Recurse -Force src-tauri/binaries/jre }
Copy-Item -Recurse $jreOut src-tauri/binaries/jre
Copy-Item server/target/riftforge-server-0.1.0.jar `
          src-tauri/binaries/riftforge-server.jar -Force
Write-Host "JRE and JAR placed in src-tauri/binaries/"

if (-not $env:VITE_BUILD_DATE) {
  $env:VITE_BUILD_DATE = (Get-Date).ToUniversalTime().ToString("yyyy-MM-dd")
}
Write-Host "Using frontend build date $env:VITE_BUILD_DATE"

npm run tauri:build -- --bundles nsis
if ($LASTEXITCODE -ne 0) { throw "Tauri build failed with exit code $LASTEXITCODE." }
Write-Host "Done. Installer is in src-tauri/target/release/bundle/"
