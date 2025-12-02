@echo off
echo ========================================
echo   Detailed Ad Blocking Monitor
echo ========================================
echo.
echo This will show:
echo - Blocked requests
echo - Direct link ads
echo - Loading statistics
echo.
echo Press Ctrl+C to stop
echo.

REM Clear previous logs
adb logcat -c

REM Watch with more detail
adb logcat -v time *:S AdBlockWebViewClient:D FastAdBlockEngine:D AdBlockMetrics:D
