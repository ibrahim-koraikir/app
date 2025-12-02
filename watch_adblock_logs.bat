@echo off
echo ========================================
echo   Ad Blocking Logcat Monitor
echo ========================================
echo.
echo Watching for ad blocking messages...
echo Press Ctrl+C to stop
echo.

REM Clear previous logs
adb logcat -c

REM Watch for ad blocking related logs
adb logcat -v time | findstr /i "AdBlock FastAdBlockEngine"
