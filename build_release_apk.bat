@echo off
echo ========================================
echo Building Release APK
echo ========================================
echo.

REM Check if signing is configured
findstr /C:"KEYSTORE_FILE" local.properties >nul 2>&1
if errorlevel 1 (
    echo WARNING: No signing config found in local.properties
    echo The APK will be signed with debug key.
    echo.
    echo To sign with release key, add these to local.properties:
    echo   KEYSTORE_FILE=path/to/your/keystore.jks
    echo   KEYSTORE_PASSWORD=your_password
    echo   KEY_ALIAS=your_alias
    echo   KEY_PASSWORD=your_password
    echo.
)

echo Building release APK...
call gradlew assembleRelease

if errorlevel 1 (
    echo.
    echo BUILD FAILED!
    pause
    exit /b 1
)

echo.
echo ========================================
echo BUILD SUCCESSFUL!
echo ========================================
echo.
echo APK files are located at:
echo   app\build\outputs\apk\release\
echo.
echo Available APKs:
dir /b app\build\outputs\apk\release\*.apk 2>nul
echo.
echo For universal APK (all architectures), enable universalApk in build.gradle.kts
echo.

REM Open the folder
explorer app\build\outputs\apk\release

pause
