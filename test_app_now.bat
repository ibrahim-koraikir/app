@echo off
echo ========================================
echo Testing App - Open the app now!
echo ========================================
echo.
echo 1. Open the Entertainment Browser app on your device
echo 2. Try clicking on a website
echo 3. Wait 5 seconds...
echo.
timeout /t 5 /nobreak
echo.
echo Checking for crashes...
echo ========================================
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" logcat -d AndroidRuntime:E *:S | findstr "entertainmentbrowser"
echo.
echo ========================================
echo If you see errors above, the app crashed.
echo If nothing appears, the app is working!
echo ========================================
pause
