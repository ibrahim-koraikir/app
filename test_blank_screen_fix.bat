@echo off
echo ========================================
echo   Testing Blank Screen Fix
echo ========================================
echo.
echo This script monitors WebView loading to diagnose blank screen issues
echo.
echo Press Ctrl+C to stop
echo.

REM Clear previous logs
adb logcat -c

echo Monitoring WebView logs...
echo.

REM Watch for WebView loading and errors
adb logcat -v time | findstr /i "CustomWebView WebViewViewModel AdBlockWebViewClient onPageStarted onPageFinished loadUrl"
