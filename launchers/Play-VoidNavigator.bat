@echo off
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
    echo VoidNavigator needs Java 17 or newer to run this portable download.
    echo Install a Java runtime, then open Play-VoidNavigator.bat again.
    pause
    exit /b 1
)
java -jar AsteroidGame.jar
if errorlevel 1 pause
