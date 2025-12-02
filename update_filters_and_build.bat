@echo off
echo ========================================
echo Ad Filter Update and Build Script
echo ========================================
echo.

echo Step 1: Downloading latest filter lists...
echo.

echo Downloading EasyList...
curl -o app/src/main/assets/adblock/easylist.txt https://easylist.to/easylist/easylist.txt
if %errorlevel% neq 0 (
    echo WARNING: Failed to download EasyList
)

echo.
echo Downloading EasyPrivacy...
curl -o app/src/main/assets/adblock/easyprivacy.txt https://easylist.to/easylist/easyprivacy.txt
if %errorlevel% neq 0 (
    echo WARNING: Failed to download EasyPrivacy
)

echo.
echo Downloading Fanboy Annoyance List...
curl -o app/src/main/assets/adblock/fanboy-annoyance.txt https://secure.fanboy.co.nz/fanboy-annoyance.txt
if %errorlevel% neq 0 (
    echo WARNING: Failed to download Fanboy Annoyance
)

echo.
echo ========================================
echo Step 2: Building APK with updated filters...
echo ========================================
echo.

gradlew.bat assembleDebug

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo SUCCESS! APK built with updated filters
    echo ========================================
    echo.
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo To install on device:
    echo   adb install -r app\build\outputs\apk\debug\app-debug.apk
    echo.
) else (
    echo.
    echo ========================================
    echo BUILD FAILED
    echo ========================================
    echo.
)

pause
