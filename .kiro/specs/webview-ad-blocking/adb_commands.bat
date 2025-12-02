@echo off
REM Ad-Blocking Testing - ADB Commands Helper Script (Windows)
REM This script provides convenient commands for testing ad-blocking functionality

setlocal enabledelayedexpansion
set PACKAGE=com.entertainmentbrowser

:MAIN_MENU
cls
echo ========================================
echo Ad-Blocking Testing - ADB Helper
echo ========================================
echo.

REM Check if device is connected
adb devices | findstr "device" >nul
if errorlevel 1 (
    echo [ERROR] No device connected
    echo Please connect a device or start an emulator
    pause
    exit /b 1
)
echo [OK] Device connected
echo.

echo Select an option:
echo 1. Install debug APK
echo 2. Clear app data
echo 3. Monitor ad-blocking logs (real-time)
echo 4. Monitor performance metrics
echo 5. Check memory usage
echo 6. Measure filter load time
echo 7. Enable GPU profiling
echo 8. Disable GPU profiling
echo 9. Capture full logs to file
echo 10. Take screenshot
echo 11. Generate test report data
echo 12. Run all performance tests
echo 0. Exit
echo.

set /p choice="Enter choice: "

if "%choice%"=="1" goto INSTALL_APK
if "%choice%"=="2" goto CLEAR_DATA
if "%choice%"=="3" goto MONITOR_LOGS
if "%choice%"=="4" goto MONITOR_PERFORMANCE
if "%choice%"=="5" goto CHECK_MEMORY
if "%choice%"=="6" goto MEASURE_LOAD_TIME
if "%choice%"=="7" goto ENABLE_GPU
if "%choice%"=="8" goto DISABLE_GPU
if "%choice%"=="9" goto CAPTURE_LOGS
if "%choice%"=="10" goto TAKE_SCREENSHOT
if "%choice%"=="11" goto GENERATE_REPORT
if "%choice%"=="12" goto RUN_ALL_TESTS
if "%choice%"=="0" goto EXIT

echo Invalid option
pause
goto MAIN_MENU

:INSTALL_APK
echo.
echo [INFO] Installing debug APK...
cd ..\..\..
call gradlew.bat installDebug
if errorlevel 1 (
    echo [ERROR] Failed to install APK
) else (
    echo [OK] APK installed successfully
)
pause
goto MAIN_MENU

:CLEAR_DATA
echo.
echo [INFO] Clearing app data...
adb shell pm clear %PACKAGE%
if errorlevel 1 (
    echo [ERROR] Failed to clear app data
) else (
    echo [OK] App data cleared
)
pause
goto MAIN_MENU

:MONITOR_LOGS
echo.
echo [INFO] Monitoring ad-blocking logs (Ctrl+C to stop)...
echo Watching: FastAdBlockEngine, AdBlockWebViewClient, AdBlockMetrics
echo.
adb logcat -c
adb logcat -s FastAdBlockEngine:D AdBlockWebViewClient:D AdBlockMetrics:D
pause
goto MAIN_MENU

:MONITOR_PERFORMANCE
echo.
echo [INFO] Monitoring performance metrics...
echo.
adb logcat -c
echo Press Enter after navigating to test pages...
pause >nul
echo.
echo Collecting metrics...
echo.

echo Filter Load Time:
adb logcat -d | findstr "Loaded in"
echo.

echo Blocked Requests:
adb logcat -d | findstr "Page finished"
echo.

echo Memory Usage:
adb shell dumpsys meminfo %PACKAGE% | findstr "TOTAL PSS"
echo.

pause
goto MAIN_MENU

:CHECK_MEMORY
echo.
echo [INFO] Checking memory usage...
echo.
echo Current memory usage:
adb shell dumpsys meminfo %PACKAGE% | findstr /C:"App Summary" /A:20
echo.
echo Total PSS:
adb shell dumpsys meminfo %PACKAGE% | findstr "TOTAL PSS"
echo.
pause
goto MAIN_MENU

:MEASURE_LOAD_TIME
echo.
echo [INFO] Measuring filter load time...
echo.
echo Clearing app data for cold start...
adb shell pm clear %PACKAGE%
adb logcat -c
echo.
echo Please launch the app now...
echo Waiting for filter load (30 second timeout)...
echo.

REM Wait for load message (simplified for Windows)
timeout /t 30 /nobreak >nul
adb logcat -d -s FastAdBlockEngine:D | findstr "Loaded in"

if errorlevel 1 (
    echo [ERROR] Timeout waiting for filter load
) else (
    echo [OK] Filter load completed
    echo.
    echo Full statistics:
    adb logcat -d -s FastAdBlockEngine:D | findstr /C:"Loaded in" /A:3
)
pause
goto MAIN_MENU

:ENABLE_GPU
echo.
echo [INFO] Enabling GPU profiling...
adb shell setprop debug.hwui.profile visual_bars
echo [OK] GPU profiling enabled
echo.
echo You should now see colored bars on screen showing frame rendering time
echo Green bars below red line = 60fps (good)
echo Bars above red line = frame drops (bad)
pause
goto MAIN_MENU

:DISABLE_GPU
echo.
echo [INFO] Disabling GPU profiling...
adb shell setprop debug.hwui.profile false
echo [OK] GPU profiling disabled
pause
goto MAIN_MENU

:CAPTURE_LOGS
echo.
set TIMESTAMP=%date:~-4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set FILENAME=adblock_logs_%TIMESTAMP%.txt

echo [INFO] Capturing logs to %FILENAME%...
adb logcat -d > "%FILENAME%"

if errorlevel 1 (
    echo [ERROR] Failed to capture logs
) else (
    echo [OK] Logs saved to %FILENAME%
    for %%A in ("%FILENAME%") do echo File size: %%~zA bytes
)
pause
goto MAIN_MENU

:TAKE_SCREENSHOT
echo.
set TIMESTAMP=%date:~-4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set FILENAME=screenshot_%TIMESTAMP%.png

echo [INFO] Taking screenshot...
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png "%FILENAME%"
adb shell rm /sdcard/screenshot.png

if errorlevel 1 (
    echo [ERROR] Failed to take screenshot
) else (
    echo [OK] Screenshot saved to %FILENAME%
)
pause
goto MAIN_MENU

:GENERATE_REPORT
echo.
set TIMESTAMP=%date:~-4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set REPORT_FILE=test_data_%TIMESTAMP%.txt

echo [INFO] Generating test report data...

(
    echo =========================================
    echo Ad-Blocking Test Data
    echo Generated: %date% %time%
    echo =========================================
    echo.
    
    echo FILTER LOAD TIME:
    adb logcat -d -s FastAdBlockEngine:D | findstr "Loaded in"
    echo.
    
    echo FILTER STATISTICS:
    adb logcat -d -s FastAdBlockEngine:D | findstr "Blocked domains"
    echo.
    
    echo BLOCKED REQUESTS (Last 10 pages):
    adb logcat -d -s AdBlockWebViewClient:D | findstr "Page finished"
    echo.
    
    echo MEMORY USAGE:
    adb shell dumpsys meminfo %PACKAGE% | findstr "TOTAL PSS"
    echo.
    
    echo SAMPLE BLOCKED URLS (Last 20):
    adb logcat -d | findstr "Blocked by"
    echo.
) > "%REPORT_FILE%"

echo [OK] Test data saved to %REPORT_FILE%
echo Use this data to fill out the test report template
pause
goto MAIN_MENU

:RUN_ALL_TESTS
echo.
echo [INFO] Running all performance tests...
echo.

echo Test 1/4: Filter Load Time
call :MEASURE_LOAD_TIME_SILENT
timeout /t 2 /nobreak >nul

echo.
echo Test 2/4: Memory Usage
echo Navigate to 5 different websites, then press Enter...
pause >nul
adb shell dumpsys meminfo %PACKAGE% | findstr "TOTAL PSS"
timeout /t 2 /nobreak >nul

echo.
echo Test 3/4: Blocked Requests
adb logcat -d | findstr "Page finished"
timeout /t 2 /nobreak >nul

echo.
echo Test 4/4: Performance Summary
adb logcat -d -s FastAdBlockEngine:D | findstr /C:"Loaded in" /A:3

echo.
echo [OK] All tests completed
pause
goto MAIN_MENU

:MEASURE_LOAD_TIME_SILENT
adb shell pm clear %PACKAGE% >nul 2>&1
adb logcat -c >nul 2>&1
echo Please launch the app now...
timeout /t 30 /nobreak >nul
adb logcat -d -s FastAdBlockEngine:D | findstr "Loaded in"
goto :EOF

:EXIT
echo.
echo Goodbye!
exit /b 0
