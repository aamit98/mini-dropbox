# ----- run-client.ps1 -----
param(
  [string]$remoteHost = "127.0.0.1",
  [int]$remotePort = 7777
)

$ErrorActionPreference = "Stop"

# 0) Run from this script's folder (so mvn uses this pom.xml)
Set-Location -Path $PSScriptRoot

# 1) Ensure JAVA_HOME
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
  $jdk = "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
  if (-not (Test-Path $jdk)) {
    $jdk = (Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory |
            Where-Object { $_.Name -like "jdk-17*" } |
            Select-Object -ExpandProperty FullName -First 1)
  }
  if ($jdk) { $env:JAVA_HOME = $jdk }
}

# 2) Find Maven (prefer Scoop)
$mvn = "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd"
if (Test-Path $mvn) {
  $env:Path = "$($env:JAVA_HOME)\bin;$(Split-Path $mvn);$env:Path"
} else {
  $env:Path = "$($env:JAVA_HOME)\bin;$env:Path"
  $mvn = "mvn"
}

# 3) Validate tools
if (-not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
  Write-Error "JAVA_HOME is not set correctly. Expected java at '$env:JAVA_HOME\bin\java.exe'."
  exit 1
}
try { & $mvn -v | Out-Null } catch {
  Write-Error "Maven not found. Install via Scoop (scoop install maven) or ensure 'mvn' is on PATH."
  exit 1
}

# 4) Build client
Write-Host "Building client..." -ForegroundColor Cyan
& $mvn -q clean compile

# 5) Run
Write-Host "Starting TftpClient $remoteHost $remotePort" -ForegroundColor Green
& "$env:JAVA_HOME\bin\java.exe" -cp "target\classes" bgu.spl.net.impl.tftp.TftpClient $remoteHost $remotePort
