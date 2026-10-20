@echo off
setlocal

echo ================================================
echo        Building ProjectTrack LAN

echo ================================================

if exist out rmdir /s /q out
if not exist dist mkdir dist
mkdir out

dir /s /b src\*.java > sources.txt
if errorlevel 1 (
    echo Could not enumerate Java source files.
    pause
    exit /b 1
)

echo Compiling Java source...
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo.
    echo BUILD FAILED.
    del /q sources.txt 2>nul
    pause
    exit /b 1
)

echo Creating client JAR...
jar cfe dist\ProjectTrackClient.jar projecttrack.client.Main -C out .
if errorlevel 1 goto jarfail

echo Creating server JAR...
jar cfe dist\ProjectTrackServer.jar projecttrack.server.ServerMain -C out .
if errorlevel 1 goto jarfail

echo Creating smoke-test JAR...
jar cfe dist\ProjectTrackLAN_SmokeTest.jar projecttrack.server.SmokeTestMain -C out .
if errorlevel 1 goto jarfail

del /q sources.txt 2>nul

echo.
echo ================================================
echo        BUILD SUCCESSFUL
echo ================================================
echo Client: dist\ProjectTrackClient.jar
echo Server: dist\ProjectTrackServer.jar
echo Smoke:  dist\ProjectTrackLAN_SmokeTest.jar
pause
exit /b 0

:jarfail
echo.
echo JAR CREATION FAILED.
del /q sources.txt 2>nul
pause
exit /b 1
