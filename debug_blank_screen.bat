@echo off
echo ========================================
echo Debug Blank Screen Issue
echo ========================================
echo.

echo Checking logcat for errors...
echo.

echo 1. Checking if AdvancedEngine initialized:
adb logcat -d | findstr "Advanced ad-blocker ready"

echo.
echo 2. Checking for crashes:
adb logcat -d | findstr /C:"FATAL" /C:"AndroidRuntime"

echo.
echo 3. Checking for WebView errors:
adb logcat -d | findstr /C:"WebView" /C:"CustomWebView" /C:"AdBlockWebViewClient"

echo.
echo 4. Checking what's being blocked:
adb logcat -d | findstr "Blocked"

echo.
echo ========================================
echo If you see errors above, that's the problem.
echo If no errors, the engines might not be initialized.
echo ========================================
pause
