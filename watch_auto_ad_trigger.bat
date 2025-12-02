@echo off
echo ========================================
echo Monitoring Auto-Ad Trigger System
echo ========================================
echo.
echo This will show:
echo - URL load counter (X / threshold)
echo - When threshold is reached
echo - What URL is being opened
echo - Tab creation events
echo.
echo Press Ctrl+C to stop
echo ========================================
echo.

adb logcat -c
adb logcat -v time ^
    AdBlockWebViewClient:D ^
    WebViewViewModel:D ^
    WebViewScreen:D ^
    CustomWebView:D ^
    *:S
