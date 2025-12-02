@echo off
echo Getting crash logs...
adb logcat -d *:E | findstr "FATAL AndroidRuntime"
echo.
echo Full crash details:
adb logcat -d AndroidRuntime:E *:S
