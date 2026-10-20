@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

echo ==========================================================
echo     ProjectTrack LAN - Windows Bundled Runtime Build
echo ==========================================================
echo.

if not exist dist\ProjectTrackClient.jar (
    echo ProjectTrackClient.jar not found. Building project first...
    call build.bat
    if errorlevel 1 exit /b 1
)

if not exist dist\ProjectTrackServer.jar (
    echo ProjectTrackServer.jar not found. Building project first...
    call build.bat
    if errorlevel 1 exit /b 1
)

set "JPACKAGE=jpackage"
where jpackage >nul 2>&1
if errorlevel 1 (
    if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" (
        set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
    ) else (
        echo.
        echo ERROR: jpackage was not found.
        echo Install a JDK 21 or newer and make sure its bin folder is on PATH,
        echo or set JAVA_HOME to the JDK installation folder.
        echo.
        pause
        exit /b 1
    )
)

if exist release rmdir /s /q release
mkdir release

echo.
echo Creating ProjectTrack Client app-image...
"%JPACKAGE%" ^
  --type app-image ^
  --name "ProjectTrack Client" ^
  --input dist ^
  --main-jar ProjectTrackClient.jar ^
  --main-class projecttrack.client.Main ^
  --dest release ^
  --app-version 1.0.0 ^
  --vendor "ProjectTrack" ^
  --description "LAN ticket-based project tracking client" ^
  --icon resources\ProjectTrack.ico ^
  --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
    echo.
    echo CLIENT PACKAGING FAILED.
    pause
    exit /b 1
)

echo.
echo Creating ProjectTrack Server app-image...
"%JPACKAGE%" ^
  --type app-image ^
  --name "ProjectTrack Server" ^
  --input dist ^
  --main-jar ProjectTrackServer.jar ^
  --main-class projecttrack.server.ServerMain ^
  --dest release ^
  --app-version 1.0.0 ^
  --vendor "ProjectTrack" ^
  --description "LAN ticket-based project tracking server" ^
  --icon resources\ProjectTrack_Server.ico ^
  --win-console ^
  --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
    echo.
    echo SERVER PACKAGING FAILED.
    pause
    exit /b 1
)

if exist data (
    echo.
    echo Copying server data folder...
    xcopy /E /I /Y data "release\ProjectTrack Server\data" >nul
)

echo.
echo Writing package readme files...
copy /Y docs\CLIENT_PACKAGE_README.txt "release\ProjectTrack Client\README.txt" >nul
copy /Y docs\SERVER_PACKAGE_README.txt "release\ProjectTrack Server\README.txt" >nul

echo.
echo ==========================================================
echo  BUNDLED-RUNTIME PACKAGING SUCCESSFUL
echo ==========================================================
echo.
echo Client executable:
echo   release\ProjectTrack Client\ProjectTrack Client.exe
echo.
echo Server executable:
echo   release\ProjectTrack Server\ProjectTrack Server.exe
echo.
echo Each app-image contains its own Java runtime.
echo Client PCs do NOT need Java installed.
echo.
echo NOTE:
echo This script must be run on Windows because jpackage creates
echo Windows app images using the Windows runtime and launcher.
echo.
pause
exit /b 0
