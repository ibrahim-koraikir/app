@echo off
echo ========================================
echo   Testing Direct Link Ad Blocking
echo ========================================
echo.
echo This script will:
echo 1. Install the updated app
echo 2. Monitor for direct link ad blocking
echo.
echo Press Ctrl+C to stop
echo.

echo Installing app...
call gradlew installDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Failed to install app
    pause
    exit /b 1
)

echo.
echo ========================================
echo   App installed successfully!
echo ========================================
echo.
echo Now monitoring logs for direct link ads...
echo.
echo What to look for:
echo   - "🚫 Blocked sponsored link:" messages
echo   - "🎯 Direct link ads blocked: X" (should be > 0 now!)
echo.
echo Test sites:
echo   - adblock-tester.com
echo   - a.asd.homes (Arabic site with many ads)
echo   - forbes.com
echo   - businessinsider.com
echo.
echo ========================================
echo.

REM Clear logs
adb logcat -c

REM Watch for ad blocking with focus on direct link ads
adb logcat -v time | findstr /i "Direct Blocked sponsored AdBlockWebViewClient"
