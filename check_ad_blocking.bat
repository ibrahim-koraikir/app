@echo off
echo ========================================
echo Ad Blocking Analysis
echo ========================================
echo.
echo This will show you what's being blocked vs allowed
echo.
echo Connect your device and press any key...
pause > nul
echo.
echo Clearing logs...
adb logcat -c
echo.
echo Starting log capture (30 seconds)...
echo Navigate to a website with ads now...
echo.
timeout /t 30 /nobreak
echo.
echo ========================================
echo ANALYSIS RESULTS
echo ========================================
echo.

echo Total ad blocking checks:
adb logcat -d | find /c "shouldBlock"

echo.
echo Blocked requests:
adb logcat -d | find "Blocked" | find /c "FastEngine"

echo.
echo Allowed requests:
adb logcat -d | find "Allowed" | find /c "AdBlockMetrics"

echo.
echo ========================================
echo Recent blocked domains:
echo ========================================
adb logcat -d | find "Blocked" | find "FastEngine"

echo.
echo ========================================
echo Ads that got through (if any):
echo ========================================
adb logcat -d | find "Allowed" | find "ad"

echo.
pause
