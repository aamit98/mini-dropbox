# start-all.ps1 - Start all services for Mini Dropbox
$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Mini Dropbox - Starting All Services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$projectRoot = $PSScriptRoot

# Start REST server in background
Write-Host "[1/2] Starting REST server..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-File", "$projectRoot\rest-server\run-server.ps1" -WindowStyle Minimized

# Wait a bit for server to start
Start-Sleep -Seconds 5

# Start UI
Write-Host "[2/2] Starting UI..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-File", "$projectRoot\ui\run-ui.ps1"

Write-Host ""
Write-Host "Services started!" -ForegroundColor Green
Write-Host "  - REST API: http://localhost:8080" -ForegroundColor Cyan
Write-Host "  - UI: http://localhost:5173" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press any key to stop all services..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

