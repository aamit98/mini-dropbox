# server/run-server.ps1
$ErrorActionPreference = "Stop"

# ---- Java & Maven setup (best-effort) ----
if (-not $env:JAVA_HOME) {
  $jdk = "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
  if (-not (Test-Path $jdk)) {
    $jdk = (Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory |
            Where-Object { $_.Name -like "jdk-17*" } |
            Select-Object -ExpandProperty FullName -First 1)
  }
  if ($jdk) { $env:JAVA_HOME = $jdk }
}
$mvn = "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd"
if (Test-Path $mvn) { $env:Path = "$($env:JAVA_HOME)\bin;$([System.IO.Path]::GetDirectoryName($mvn));$env:Path" }
else { $env:Path = "$($env:JAVA_HOME)\bin;$env:Path" }

# ---- Shared config (match REST application.properties) ----
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptPath
$base     = Join-Path $projectRoot "server\Files"
$teleHost = "127.0.0.1"
$telePort = 9099
$port     = if ($args.Length -ge 1) { $args[0] } else { 7777 }

Write-Host "Building TFTP server..." -ForegroundColor Cyan
& mvn -q clean compile

Write-Host "Ensuring base dir exists: $base" -ForegroundColor Cyan
if (-not (Test-Path $base)) { New-Item -ItemType Directory -Path $base | Out-Null }

Write-Host "Starting TftpServer on port $port" -ForegroundColor Green

# Build a clean argument list for Maven without --% and without PowerShell backticks
$mvArgs = @(
  'exec:java',
  "-Dexec.mainClass=bgu.spl.net.impl.tftp.TftpServer",
  "-Dexec.args=$port",
  "-Dstorage.base-dir=$base",
  "-Dtelemetry.host=$teleHost",
  "-Dtelemetry.port=$telePort"
)

# Call Maven with arguments. PowerShell will pass each item as a single arg (spaces preserved).
& mvn -q @mvArgs
