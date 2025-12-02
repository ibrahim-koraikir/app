@echo off
echo ===================================================
echo      Ad-Blocker Upgrade Verification Script
echo ===================================================
echo.
echo 1. Building the app...
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed! Please check the logs.
    exit /b %ERRORLEVEL%
)
echo [SUCCESS] Build successful!
echo.
echo 2. Installation Instructions:
echo    - Connect your Android device via USB.
echo    - Run: adb install -r app\build\outputs\apk\debug\app-debug.apk
echo.
echo 3. Verification Steps:
echo    A. Blocking Rate:
echo       - Open the app.
echo       - Go to: https://d3ward.github.io/toolz/adblock.html
echo       - Run the test. Target: 95%%+
echo.
echo    B. False Positives (Critical Apps):
echo       - Login to Google/Facebook.
echo       - Play a YouTube video.
echo       - Check a banking site (e.g., PayPal).
echo       - Verify NO breakage.
echo.
echo    C. Redirects & Popups:
echo       - Visit a site known for popups.
echo       - Verify they are blocked or show the BLACK "Ad Blocked" screen.
echo.
echo    D. WebRTC Leak:
echo       - Go to: https://browserleaks.com/webrtc
echo       - Verify WebRTC is disabled or not leaking local IP.
echo.
echo ===================================================
pause
