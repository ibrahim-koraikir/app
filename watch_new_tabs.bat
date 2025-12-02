@echo off
echo ========================================
echo New Tab Prevention Monitor
echo ========================================
echo.
echo Watching for:
echo - onCreateWindow attempts
echo - Multiple window requests
echo - Tab creation events
echo.
echo Press Ctrl+C to stop
echo ========================================
echo.

adb logcat -c
adb logcat | findstr /I "onCreateWindow CustomWebView.*window TabManager.*create"
