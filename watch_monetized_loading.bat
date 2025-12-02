@echo off
echo ========================================
echo Monitoring Monetized Tab Loading
echo ========================================
echo.
echo This will show:
echo - Page load events (start/finish)
echo - Network errors (cleartext, timeouts, etc.)
echo - Redirect chains
echo - Ad blocking status
echo.
echo Press Ctrl+C to stop
echo ========================================
echo.

adb logcat -c
adb logcat -v time ^
    AdBlockWebViewClient:D ^
    WebViewViewModel:D ^
    CustomWebView:D ^
    chromium:W ^
    *:S
