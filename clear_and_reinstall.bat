@echo off
echo ========================================
echo Clear App Data and Reinstall
echo ========================================
echo.
echo This will:
echo 1. Clear app data (including database)
echo 2. Reinstall the app
echo 3. Images will load from web URLs
echo.
pause

echo.
echo [1/3] Clearing app data...
adb shell pm clear com.entertainmentbrowser
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to clear app data
    echo Make sure device is connected and USB debugging is enabled
    pause
    exit /b 1
)

echo.
echo [2/3] Installing APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to install APK
    pause
    exit /b 1
)

echo.
echo [3/3] Launching app...
adb shell am start -n com.entertainmentbrowser/.MainActivity

echo.
echo ========================================
echo SUCCESS!
echo ========================================
echo.
echo The app has been reinstalled with fresh data.
echo Website logos will now load from the web.
echo.
pause
