Write-Host " Cleaning everything..." -ForegroundColor Yellow

# Stop Gradle daemon
& .\gradlew.bat --stop

# Force stop app
adb shell am force-stop com.bodepudi.vexnotebooktracker

# Clear app data
adb shell pm clear com.bodepudi.vexnotebooktracker

# Clean build
Write-Host " Running Gradle clean..." -ForegroundColor Yellow
& .\gradlew.bat clean

# Remove build artifacts (with error handling)
Write-Host "Removing build directories..." -ForegroundColor Yellow
if (Test-Path ".gradle") { 
    Remove-Item -Path .gradle -Recurse -Force
    Write-Host "   Removed .gradle" -ForegroundColor Green
}
if (Test-Path "build") { 
    Remove-Item -Path build -Recurse -Force 
    Write-Host "   Removed build" -ForegroundColor Green
}
if (Test-Path "app\build") { 
    Remove-Item -Path app\build -Recurse -Force 
    Write-Host "   Removed app\build" -ForegroundColor Green
}

# Uninstall app (ignore errors if not installed)
Write-Host " Uninstalling existing app..." -ForegroundColor Yellow
try {
    adb uninstall com.bodepudi.vexnotebooktracker 2>$null
    Write-Host "   App uninstalled" -ForegroundColor Green
} catch {
    Write-Host "   App was not installed" -ForegroundColor DarkYellow
}

# Build build
Write-Host " Building build APK..." -ForegroundColor Yellow
& .\gradlew.bat build

if ($LASTEXITCODE -eq 0) {
    Write-Host "   Build successful!" -ForegroundColor Green
    
    # Install build APK
    Write-Host " Installing build APK..." -ForegroundColor Yellow
    adb install "app/build/outputs/apk/debug/app-debug.apk"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host " build app deployed successfully!" -ForegroundColor Green
        Write-Host " You can now test the build version on your device." -ForegroundColor Cyan
    } else {
        Write-Host " Installation failed" -ForegroundColor Red
    }
} else {
    Write-Host "❌ Build failed" -ForegroundColor Red
}

Write-Host "`nPress any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")