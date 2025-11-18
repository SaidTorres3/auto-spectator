# PowerShell script to build the Minecraft plugin

param(
    [switch]$Clean = $false,
    [switch]$Package = $false
)

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Building Minecraft Plugin..." -ForegroundColor Cyan

# Check if Maven is installed
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "Error: Maven is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

# Build command
$buildArgs = @("clean", "package", "-DskipTests")

if ($Clean) {
    Write-Host "Clean build enabled" -ForegroundColor Yellow
}

if ($Package) {
    Write-Host "Packaging enabled" -ForegroundColor Yellow
}

# Change to project directory
Push-Location $projectRoot

try {
    Write-Host "Running Maven build..." -ForegroundColor Green
    & mvn $buildArgs
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Build completed successfully!" -ForegroundColor Green
        Write-Host "Plugin JAR: $projectRoot\target\auto-spectator-1.0-SNAPSHOT.jar" -ForegroundColor Cyan
    }
    else {
        Write-Host "Build failed with exit code: $LASTEXITCODE" -ForegroundColor Red
        exit 1
    }
}
finally {
    Pop-Location
}
