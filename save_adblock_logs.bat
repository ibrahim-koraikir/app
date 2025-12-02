@echo off
echo ========================================
echo   Save Ad Blocking Logs to File
echo ========================================
echo.

REM Create logs directory if it doesn't exist
if not exist "logs" mkdir logs

REM Generate filename with timestamp
set timestamp=%date:~-4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set timestamp=%timestamp: =0%
set logfile=logs\adblock_log_%timestamp%.txt

echo Saving logs to: %logfile%
echo.
echo Press Ctrl+C to stop logging
echo.

REM Clear and start logging
adb logcat -c
adb logcat -v time | findstr /i "AdBlock FastAdBlockEngine" > %logfile%
