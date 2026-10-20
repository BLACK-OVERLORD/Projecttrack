@echo off
echo Adding Windows Defender Firewall rule for ProjectTrack TCP port 5050...
netsh advfirewall firewall add rule name="ProjectTrack LAN 5050" dir=in action=allow protocol=TCP localport=5050
if errorlevel 1 (
    echo.
    echo Failed. Please run this batch file as Administrator.
    pause
    exit /b 1
)
echo Firewall rule added. Use a Private LAN profile for the server network.
pause
