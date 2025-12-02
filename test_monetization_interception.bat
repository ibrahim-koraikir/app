@echo off
echo ========================================
echo Testing Monetization Interception System
echo ========================================
echo.
echo This will show monetization logs in real-time.
echo.
echo What to look for:
echo   - "URL load tracked: X/Y" - Progress toward ad
echo   - "Should show ad" - Ad will be shown next
echo   - "Intercepting navigation" - Ad is being displayed
echo   - "Ad viewed, continuing to pending URL" - Navigating to destination
echo.
echo Instructions:
echo   1. Open the app on your device
echo   2. Click on 3-6 different videos/links
echo   3. Watch the logs below
echo   4. You should see ad interception happen
echo.
echo Press Ctrl+C to stop
echo ========================================
echo.

adb logcat -c
adb logcat | findstr /C:"MonetizationManager" /C:"WebViewViewModel"
