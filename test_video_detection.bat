@echo off
echo Watching for video detection logs...
echo.
adb logcat -v time | findstr /i "VideoDetector CustomWebView AndroidInterface video"
