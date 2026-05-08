@echo off
setlocal

echo [1/2] Building Fat JAR with Maven...
call mvn clean package

if errorlevel 1 (
    echo Maven build failed.
    pause
    exit /b 1
)

echo [2/2] Creating Windows Installer (.exe)...

:: Default to jpackage in PATH
set JP_CMD=jpackage

:: If not in path, try JAVA_HOME
where jpackage >nul 2>nul
if errorlevel 1 (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\jpackage.exe" set JP_CMD="%JAVA_HOME%\bin\jpackage.exe"
    )
)

%JP_CMD% --input target/ ^
         --name zDwnld ^
         --main-jar zDwnld-1.0-SNAPSHOT.jar ^
         --main-class opensource.DlacInc.ZDwnld.ZDwnld ^
         --type exe ^
         --win-dir-chooser ^
         --win-shortcut ^
         --icon Icon.png ^
         --app-version 1.0.0 ^
         --vendor "DlacInc" ^
         --description "High Speed Download Manager"

if errorlevel 1 (
    echo.
    echo ERROR: jpackage failed or was not found.
    echo 1. Ensure you have a JDK installed (not just JRE).
    echo 2. Ensure you have the WiX Toolset installed: https://wixtoolset.org/
    pause
    exit /b 1
)

echo Success! Your all-in-one installer is in the current directory.
pause
