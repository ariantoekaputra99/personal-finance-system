# Personal Finance System - Maven Build and Start Script

Write-Host "=== Personal Finance System - Maven Build ===" -ForegroundColor Green

# Function to check if Maven is available
function Test-Maven {
    try {
        if (Get-Command mvn -ErrorAction SilentlyContinue) {
            Write-Host "Using system Maven" -ForegroundColor Yellow
            return "mvn"
        } elseif (Test-Path ".\mvnw.cmd") {
            Write-Host "Using Maven Wrapper" -ForegroundColor Yellow
            return ".\mvnw.cmd"
        } else {
            Write-Host "Maven not found. Please install Maven or ensure mvnw.cmd is present." -ForegroundColor Red
            exit 1
        }
    } catch {
        Write-Host "Error checking Maven availability: $_" -ForegroundColor Red
        exit 1
    }
}

# Get Maven command
$mvnCmd = Test-Maven

Write-Host "Building all services..." -ForegroundColor Cyan

# Clean and compile all services
Write-Host "Cleaning previous builds..." -ForegroundColor Yellow
& $mvnCmd clean

Write-Host "Compiling all services..." -ForegroundColor Yellow
& $mvnCmd compile

Write-Host "Running tests..." -ForegroundColor Yellow
& $mvnCmd test

Write-Host "Packaging all services..." -ForegroundColor Yellow
& $mvnCmd package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build completed successfully!" -ForegroundColor Green
    
    Write-Host "`n=== Available Maven Commands ===" -ForegroundColor Green
    Write-Host "Build all services:           $mvnCmd clean package" -ForegroundColor White
    Write-Host "Run tests:                    $mvnCmd test" -ForegroundColor White
    Write-Host "Run specific service:         $mvnCmd spring-boot:run -pl <service-name>" -ForegroundColor White
    Write-Host "Install to local repository:  $mvnCmd install" -ForegroundColor White
    
    Write-Host "`n=== Service Names ===" -ForegroundColor Green
    Write-Host "- user-service" -ForegroundColor White
    Write-Host "- transaction-service" -ForegroundColor White
    Write-Host "- analytics-service" -ForegroundColor White
    Write-Host "- notification-service" -ForegroundColor White
    Write-Host "- api-gateway" -ForegroundColor White
    
    Write-Host "`n=== Example: Run User Service ===" -ForegroundColor Green
    Write-Host "$mvnCmd spring-boot:run -pl user-service" -ForegroundColor White
    
} else {
    Write-Host "Build failed! Please check the error messages above." -ForegroundColor Red
    exit 1
}