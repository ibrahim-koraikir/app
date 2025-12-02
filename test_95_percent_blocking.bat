@echo off
echo ========================================
echo 95%% Ad Blocking Test Suite
echo ========================================
echo.

echo Checking if device is connected...
adb devices
if %errorlevel% neq 0 (
    echo ERROR: No device connected
    pause
    exit /b 1
)

echo.
echo ========================================
echo Step 1: Clear logs
echo ========================================
adb logcat -c

echo.
echo ========================================
echo Step 2: Start monitoring
echo ========================================
echo.
echo Open the app and browse to test sites:
echo   - YouTube (first-party ads)
echo   - Facebook (tracking pixels)
echo   - News sites (banner ads)
echo.
echo Press Ctrl+C to stop monitoring
echo.

adb logcat | findstr /C:"AdvancedAdBlockEngine" /C:"Blocked" /C:"blocking"
