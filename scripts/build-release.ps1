#!/usr/bin/env pwsh
param(
  [switch]$Fast
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-RepoPath([string]$RelativePath) {
  return (Resolve-Path -LiteralPath (Join-Path $RepoRoot $RelativePath) -ErrorAction SilentlyContinue).Path
}

function Assert-UnderRepo([string]$PathToCheck) {
  $fullPath = [System.IO.Path]::GetFullPath($PathToCheck)
  $root = [System.IO.Path]::GetFullPath($RepoRoot)
  if (-not $fullPath.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to operate outside repository root. Path: $fullPath"
  }
}

function Remove-GeneratedPath([string]$RelativePath) {
  $path = Join-Path $RepoRoot $RelativePath
  Assert-UnderRepo $path
  if (Test-Path -LiteralPath $path) {
    Write-Host "[release] Removing $path"
    Remove-Item -LiteralPath $path -Recurse -Force
  }
}

function File-Sha256([string]$Path) {
  return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Print-FileInfo([string]$Label, [string]$Path) {
  $item = Get-Item -LiteralPath $Path
  Write-Host "[release] ${Label}: $($item.FullName)"
  Write-Host "[release] ${Label} size: $($item.Length) bytes"
  Write-Host "[release] ${Label} modified: $($item.LastWriteTimeUtc.ToString('o'))"
  Write-Host "[release] ${Label} sha256: $(File-Sha256 $item.FullName)"
}

function Run-Step([string]$Label, [scriptblock]$Step) {
  Write-Host ""
  Write-Host "[release] $Label"
  & $Step
  if ($LASTEXITCODE -ne $null -and $LASTEXITCODE -ne 0) {
    throw "$Label failed with exit code $LASTEXITCODE."
  }
}

$RepoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
Set-Location $RepoRoot

Write-Host "[release] RiftForge release build"
Write-Host "[release] Repo: $RepoRoot"
if ($Fast) {
  Write-Host "[release] Fast mode enabled: backend tests are skipped, but package/verification still run."
}

if (-not $Fast) {
  Run-Step "Running backend tests" {
    Push-Location server
    try {
      mvn -q test
    } finally {
      Pop-Location
    }
  }
}

Run-Step "Cleaning backend target and stale bundled server JAR" {
  Remove-GeneratedPath "server/target"
  $bundledJar = Join-Path $RepoRoot "src-tauri/binaries/riftforge-server.jar"
  Assert-UnderRepo $bundledJar
  if (Test-Path -LiteralPath $bundledJar) {
    Write-Host "[release] Removing old bundled server JAR $bundledJar"
    Remove-Item -LiteralPath $bundledJar -Force
  }
}

Run-Step "Building guarded desktop installer" {
  npm.cmd run build:desktop
}

$serverJars = @(Get-ChildItem -LiteralPath (Join-Path $RepoRoot "server/target") -Filter "riftforge-server-*.jar" -File |
  Where-Object { $_.Name -notlike "*.original" })
if ($serverJars.Count -ne 1) {
  $names = ($serverJars | ForEach-Object FullName) -join "`n"
  throw "Expected exactly one built server JAR, found $($serverJars.Count).`n$names"
}

$freshJar = $serverJars[0].FullName
$bundledJarPath = Join-Path $RepoRoot "src-tauri/binaries/riftforge-server.jar"
if (-not (Test-Path -LiteralPath $bundledJarPath)) {
  throw "Bundled server JAR missing after desktop build: $bundledJarPath"
}

Print-FileInfo "Fresh server JAR" $freshJar
Print-FileInfo "Bundled server JAR" $bundledJarPath

$freshHash = File-Sha256 $freshJar
$bundledHash = File-Sha256 $bundledJarPath
if ($freshHash -ne $bundledHash) {
  throw "Bundled server JAR hash mismatch. Fresh=$freshHash Bundled=$bundledHash"
}

$freshItem = Get-Item -LiteralPath $freshJar
$bundledItem = Get-Item -LiteralPath $bundledJarPath
if ($bundledItem.LastWriteTimeUtc -lt $freshItem.LastWriteTimeUtc) {
  throw "Bundled server JAR is older than the freshly built JAR."
}

Run-Step "Running standalone bundled-server verifier" {
  npm.cmd run verify:desktop-server
}

$nsisDir = Join-Path $RepoRoot "src-tauri/target/release/bundle/nsis"
$installers = @(Get-ChildItem -LiteralPath $nsisDir -Filter "RiftForge_*_x64-setup.exe" -File | Sort-Object LastWriteTimeUtc -Descending)
if ($installers.Count -lt 1) {
  throw "No NSIS installer found under $nsisDir"
}

$installer = $installers[0]
$rootInstaller = Join-Path $RepoRoot $installer.Name
Copy-Item -LiteralPath $installer.FullName -Destination $rootInstaller -Force
Print-FileInfo "NSIS installer" $installer.FullName
Print-FileInfo "Root installer copy" $rootInstaller

Write-Host ""
Write-Host "[release] Build complete."
Write-Host "[release] Fresh server JAR: $freshJar"
Write-Host "[release] Bundled server JAR: $bundledJarPath"
Write-Host "[release] Installer: $($installer.FullName)"
Write-Host "[release] Root installer copy: $rootInstaller"
