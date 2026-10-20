@echo off
cd /d "%~dp0"
if not exist dist\ProjectTrackClient.jar (
    echo ProjectTrackClient.jar not found. Run build.bat first.
    pause
    exit /b 1
)
java -jar dist\ProjectTrackClient.jar
pause
