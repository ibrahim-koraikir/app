@echo off
echo ========================================
echo Monetization Integration Test
echo ========================================
echo.
echo This will test if your monetization ads work correctly.
echo.
echo Your ad URLs:
echo 1. https://www.effectivegatecpm.com/hypsia868?key=d55fe3c96beb154d635fe6ee82094511
echo 2. https://otieu.com/4/10194754
echo.
echo ========================================
echo.

echo Step 1: Building and installing app...
call gradlew clean assembleDebug installDebug
if errorlevel 1 (
    echo.
    echo ❌ Build failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Step 2: Starting app...
echo ========================================
adb shell am start -n com.entertainmentbrowser/.MainActivity

echo.
echo ========================================
echo Step 3: Monitoring monetization activity...
echo ========================================
echo.
echo Watching for:
echo - Action tracking (counts user clicks)
echo - Ad triggers (when counter reaches 7-12)
echo - New tab creation for ads
echo - Whitelist checks (ads should NOT be blocked)
echo.
echo Instructions:
echo 1. In the app, click on 7-12 different websites
echo 2. Watch this window for monetization logs
echo 3. After 7-12 clicks, a new tab should open with your ad
echo.
echo Press Ctrl+C to stop monitoring
echo ========================================
echo.

adb logcat -c
adb logcat | findstr /I "MonetizationManager WebViewViewModel.*monetization FastAdBlockEngine.*monetization"
