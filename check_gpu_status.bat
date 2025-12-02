@echo off
echo ========================================
echo GPU Memory Status Check
echo ========================================
echo.

echo Checking device memory...
adb shell cat /proc/meminfo | findstr "MemTotal"
echo.

echo Checking GPU renderer...
adb shell dumpsys SurfaceFlinger | findstr "GLES"
echo.

echo Checking app memory usage...
adb shell dumpsys meminfo com.entertainmentbrowser | findstr "TOTAL"
echo.

echo Recent GPU errors (last 100 lines):
adb logcat -d -t 100 | findstr /I "MALI.*DEBUG GPUAUX" | find /C "BAD ALLOC"
echo.

echo GpuMemoryManager activity (last 50 lines):
adb logcat -d -t 50 | findstr "GpuMemoryManager"
echo.

echo ========================================
echo Done!
echo ========================================
