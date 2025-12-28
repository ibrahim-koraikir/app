@echo off
echo ========================================
echo Testing In-App Update Feature
echo ========================================
echo.
echo Step 1: Building and installing debug APK...
call gradlew installDebug
if errorlevel 1 (
    echo Build failed!
    pause
    exit /b 1
)
echo.
echo Step 2: Starting app and watching UpdateManager logs...
echo Press Ctrl+C to stop
echo.
adb shell am start -n com.entertainmentbrowser/.MainActivity
timeout /t 2 >nul
adb logcat -s UpdateManager:* -v time
