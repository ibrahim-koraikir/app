@echo off
echo ========================================
echo Monitoring Monetization Ad Loading
echo ========================================
echo.
echo This will show:
echo - MonetizationManager activity
echo - Ad URL loading
echo - WebView rendering
echo - Page load status
echo.
echo Press Ctrl+C to stop monitoring
echo ========================================
echo.

"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" logcat -c
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" logcat | findstr /i "MonetizationManager WebViewViewModel AdBlockMetrics CustomWebView WebViewPool chromium:I"
