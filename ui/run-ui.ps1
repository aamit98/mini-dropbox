# ui/run-ui.ps1
$ErrorActionPreference = "Stop"

# Set location to script directory
Set-Location -Path $PSScriptRoot

# Check if node_modules exists, if not install dependencies
if (-not (Test-Path "node_modules")) {
  Write-Host "Installing dependencies..." -ForegroundColor Cyan
  npm install
}

Write-Host "Starting UI development server..." -ForegroundColor Green
Write-Host "The UI will be available at http://localhost:5173" -ForegroundColor Cyan
Write-Host "Make sure the REST server is running on port 8080" -ForegroundColor Yellow

npm run dev

