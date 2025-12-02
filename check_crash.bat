@echo off
echo Checking for app crashes...
echo.
adb logcat -d AndroidRuntime:E *:S | findstr "entertainmentbrowser"
echo.
echo Checking for fatal exceptions...
adb logcat -d *:E | findstr /C:"FATAL EXCEPTION" /C:"entertainmentbrowser"
