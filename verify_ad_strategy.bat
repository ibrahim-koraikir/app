@echo off
echo Building app to verify changes...
cd /d c:\Users\w\Desktop\AndroidStudioProjects\Bro2
call gradlew.bat assembleDebug
if %ERRORLEVEL% EQU 0 (
    echo Build Successful!
) else (
    echo Build Failed! Check the output above.
)
pause
