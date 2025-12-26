# rest-server/run-server.ps1
$ErrorActionPreference = "Stop"

# Set location to script directory
Set-Location -Path $PSScriptRoot

# ---- Java & Maven setup (best-effort) ----
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
  $jdk = "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
  if (-not (Test-Path $jdk)) {
    $jdk = (Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -like "jdk-17*" } |
            Select-Object -ExpandProperty FullName -First 1)
  }
  if ($jdk) { $env:JAVA_HOME = $jdk }
}

# Find Maven
$mvn = "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd"
if (-not (Test-Path $mvn)) {
  $mvn = "mvn"
  if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Error "Maven not found. Please install Maven or ensure 'mvn' is on PATH."
    exit 1
  }
}

# Set storage directory relative to project root
$projectRoot = Split-Path -Parent $PSScriptRoot
$storageDir = Join-Path $projectRoot "server\Files"
$env:STORAGE_DIR = $storageDir

Write-Host "Building REST server..." -ForegroundColor Cyan
& $mvn -q clean compile
if ($LASTEXITCODE -ne 0) {
  Write-Error "Build failed!"
  exit 1
}

Write-Host "Starting REST server on port 8080" -ForegroundColor Green
Write-Host "Storage directory: $storageDir" -ForegroundColor Cyan
Write-Host "Access the API at: http://localhost:8080/api" -ForegroundColor Yellow
Write-Host ""

& $mvn spring-boot:run

