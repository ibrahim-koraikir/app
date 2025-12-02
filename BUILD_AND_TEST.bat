@echo off
echo ========================================
echo 95%% Ad Blocker - Build and Test
echo ========================================
echo.

echo Step 1: Building APK (this takes 3-5 minutes)...
echo.
gradlew.bat clean assembleDebug

if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo BUILD FAILED
    echo ========================================
    pause
    exit /b 1
)

echo.
echo ========================================
echo BUILD SUCCESS!
echo ========================================
echo.
echo APK location: app\build\outputs\apk\debug\app-debug.apk
echo.

echo Step 2: Installing on device...
echo.
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk

if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo INSTALL FAILED - Is device connected?
    echo ========================================
    pause
    exit /b 1
)

echo.
echo ========================================
echo INSTALL SUCCESS!
echo ========================================
echo.

echo Step 3: Checking ad blocker initialization...
echo.
echo Clearing logs...
adb logcat -c

echo.
echo Starting app... (open it on your device now)
echo.
timeout /t 5 /nobreak

echo.
echo Checking logs for ad blocker initialization...
echo.
adb logcat -d | findstr "Advanced ad-blocker ready"

echo.
echo ========================================
echo TESTING INSTRUCTIONS
echo ========================================
echo.
echo 1. Open the app on your device
echo 2. Browse to a website with ads (e.g., news site)
echo 3. Watch the console below for blocked requests
echo.
echo You should see messages like:
echo   - "Blocked first-party ad: youtube.com/api/ads/..."
echo   - "Blocked via CNAME: analytics.site.com"
echo   - "Blocked: doubleclick.net"
echo.
echo Press Ctrl+C to stop monitoring
echo.
echo ========================================
echo.

adb logcat | findstr /C:"Blocked" /C:"AdvancedAdBlockEngine"
