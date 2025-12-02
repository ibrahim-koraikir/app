@echo off
echo ========================================
echo Saving Monetization Ad Logs
echo ========================================
echo.
echo Instructions:
echo 1. This will clear the logcat and start fresh
echo 2. Perform 10 actions in the app to trigger an ad
echo 3. Wait for the ad to appear (or black screen)
echo 4. Press Ctrl+C to stop and save logs
echo.
echo Logs will be saved to: monetization_ad_logs.txt
echo ========================================
echo.
pause

adb logcat -c
echo Monitoring... Press Ctrl+C when done
adb logcat | findstr /i "MonetizationManager WebViewViewModel AdBlockMetrics CustomWebView WebViewPool chromium:I GPUAUX gralloc" > monetization_ad_logs.txt

echo.
echo ========================================
echo Logs saved to: monetization_ad_logs.txt
echo ========================================
pause
