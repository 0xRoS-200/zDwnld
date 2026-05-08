@echo off
setlocal enabledelayedexpansion

echo [1/2] Building Fat JAR with Maven...
call mvn clean package

if %ERRORLEVEL% NEQ 0 (
    echo Maven build failed.
    pause
    exit /b %ERRORLEVEL%
)

:: Try to find jpackage
set JP_CMD=jpackage
if defined JAVA_HOME (
    if exist "!JAVA_HOME!\bin\jpackage.exe" (
        set JP_CMD="!JAVA_HOME!\bin\jpackage"
    )
)

echo [2/2] Creating Windows Installer (.exe) via !JP_CMD!...
!JP_CMD! --input target/ ^
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

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: jpackage failed. 
    echo 1. Ensure you have a JDK installed (not just JRE).
    echo 2. Ensure "jpackage" is in your PATH or JAVA_HOME is set.
    echo 3. You MUST have the "WiX Toolset" installed to create an .exe installer: https://wixtoolset.org/
    pause
    exit /b %ERRORLEVEL%
)

echo Success! Your all-in-one installer is in the current directory.
pause
