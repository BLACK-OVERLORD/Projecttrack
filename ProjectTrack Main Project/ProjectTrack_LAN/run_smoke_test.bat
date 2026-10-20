@echo off
cd /d "%~dp0"
if not exist dist\ProjectTrackLAN_SmokeTest.jar (
    echo Smoke-test JAR not found. Run build.bat first.
    pause
    exit /b 1
)
java -jar dist\ProjectTrackLAN_SmokeTest.jar
pause
