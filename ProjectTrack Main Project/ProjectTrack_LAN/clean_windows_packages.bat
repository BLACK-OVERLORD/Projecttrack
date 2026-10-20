@echo off
setlocal
cd /d "%~dp0"
if exist release rmdir /s /q release
echo Windows bundled-runtime package output deleted.
pause
