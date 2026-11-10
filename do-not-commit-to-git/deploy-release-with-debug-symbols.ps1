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

# Build release APK for testing
Write-Host " Building release APK..." -ForegroundColor Yellow
& .\gradlew.bat assembleRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host "   APK build successful!" -ForegroundColor Green
    
    # Install release APK
    Write-Host " Installing release APK..." -ForegroundColor Yellow
    adb install "app/build/outputs/apk/release/app-release.apk"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   APK installed successfully!" -ForegroundColor Green
    } else {
        Write-Host "   APK installation failed" -ForegroundColor Red
    }
} else {
    Write-Host "   APK build failed" -ForegroundColor Red
    Write-Host "`nPress any key to exit..."
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit 1
}

# Build App Bundle for Play Store upload
Write-Host "`n Building App Bundle for Play Store..." -ForegroundColor Yellow
& .\gradlew.bat bundleRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host "   Bundle build successful!" -ForegroundColor Green
    
    # Check for output files
    $aabPath = "app\build\outputs\bundle\release\app-release.aab"
    $symbolsPath = "app\build\outputs\bundle\release\native-debug-symbols.zip"
    
    Write-Host "`n Output Files:" -ForegroundColor Cyan
    
    if (Test-Path $aabPath) {
        $aabSize = (Get-Item $aabPath).Length / 1MB
        Write-Host "   AAB File: $aabPath ($([math]::Round($aabSize, 2)) MB)" -ForegroundColor Green
    } else {
        Write-Host "   AAB File: NOT FOUND" -ForegroundColor Red
    }
    
    if (Test-Path $symbolsPath) {
        $symbolsSize = (Get-Item $symbolsPath).Length / 1MB
        Write-Host "   Debug Symbols: $symbolsPath ($([math]::Round($symbolsSize, 2)) MB)" -ForegroundColor Green
    } else {
        Write-Host "   Debug Symbols: NOT FOUND (This is expected if no native libraries)" -ForegroundColor Yellow
    }
    
    Write-Host "`n Release deployment complete!" -ForegroundColor Green
    Write-Host " - Test the APK on your device" -ForegroundColor Cyan
    Write-Host " - Upload the AAB to Play Console" -ForegroundColor Cyan
    
} else {
    Write-Host "   Bundle build failed" -ForegroundColor Red
}

Write-Host "`nPress any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
