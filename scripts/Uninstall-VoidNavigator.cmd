@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Uninstall-VoidNavigator.ps1"
if errorlevel 1 echo Uninstall did not complete. See the message above.
pause
