# Build script for PI_java_web project
Write-Host "Building PI_java_web project..." -ForegroundColor Green

# Try to find Maven
$mavenCmd = Get-Command mvn -ErrorAction SilentlyContinue

if ($mavenCmd) {
    Write-Host "Maven found. Building with Maven..." -ForegroundColor Yellow
    mvn clean compile
} else {
    Write-Host "Maven not found in PATH. Please build using IntelliJ IDEA:" -ForegroundColor Yellow
    Write-Host "1. Open the project in IntelliJ IDEA" -ForegroundColor Cyan
    Write-Host "2. Click Build > Build Project (Ctrl+F9)" -ForegroundColor Cyan
    Write-Host "3. Or right-click on the project and select 'Rebuild Module'" -ForegroundColor Cyan
}

Write-Host "Build process complete!" -ForegroundColor Green

