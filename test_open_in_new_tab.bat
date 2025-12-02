@echo off
echo ========================================
echo Testing "Open in New Tab" Fix
echo ========================================
echo.
echo This script will monitor logcat for URL detection during long-press
echo.
echo Instructions:
echo 1. Install and run the app on your device
echo 2. Navigate to any website with links
echo 3. Long-press on different types of links
echo 4. Select "Open in new tab"
echo 5. Watch the logs below to see URL detection
echo.
echo Press Ctrl+C to stop monitoring
echo ========================================
echo.

adb logcat -c
adb logcat | findstr /C:"CustomWebView" /C:"WebViewViewModel"
