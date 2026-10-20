@echo off
setlocal
cd /d "%~dp0"
set "OK=1"

echo ==========================================================
echo     ProjectTrack LAN - Windows Package Verification
echo ==========================================================
echo.

if not exist "release\ProjectTrack Client\ProjectTrack Client.exe" (
    echo [FAIL] Client executable not found.
    set "OK=0"
) else (
    echo [PASS] Client executable found.
)

if not exist "release\ProjectTrack Client\runtime\bin\java.exe" (
    echo [FAIL] Client bundled Java runtime not found.
    set "OK=0"
) else (
    echo [PASS] Client bundled Java runtime found.
)

if not exist "release\ProjectTrack Server\ProjectTrack Server.exe" (
    echo [FAIL] Server executable not found.
    set "OK=0"
) else (
    echo [PASS] Server executable found.
)

if not exist "release\ProjectTrack Server\runtime\bin\java.exe" (
    echo [FAIL] Server bundled Java runtime not found.
    set "OK=0"
) else (
    echo [PASS] Server bundled Java runtime found.
)

echo.
if "%OK%"=="1" (
    echo RESULT: PASS
    echo The Windows packages contain both EXEs and bundled Java runtimes.
) else (
    echo RESULT: FAIL
    echo Run package_windows.bat on Windows first.
)
echo.
pause
