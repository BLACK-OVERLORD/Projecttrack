@echo off
cd /d "%~dp0"
if not exist dist\ProjectTrackServer.jar (
    echo ProjectTrackServer.jar not found. Run build.bat first.
    pause
    exit /b 1
)
java -jar dist\ProjectTrackServer.jar
pause
