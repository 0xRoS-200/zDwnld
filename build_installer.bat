@echo off
setlocal

echo [1/2] Building Fat JAR with Maven...
call mvn clean package

if %ERRORLEVEL% NEQ 0 (
    echo Maven build failed.
    pause
    exit /b %ERRORLEVEL%
)

:: Try to find jpackage
set JP_CMD=
where jpackage >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set JP_CMD=jpackage
) else (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\jpackage.exe" (
            set JP_CMD="%JAVA_HOME%\bin\jpackage"
        )
    )
)

if "%JP_CMD%"=="" (
    echo.
    echo ERROR: jpackage was not found.
    echo 1. Ensure you have a JDK installed (not just JRE).
    echo 2. Set your JAVA_HOME environment variable to your JDK folder.
    pause
    exit /b 1
)

echo [2/2] Creating Windows Installer (.exe) via %JP_CMD%...
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

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: jpackage failed. 
    echo You MUST have the "WiX Toolset" installed to create an .exe: https://wixtoolset.org/
    pause
    exit /b %ERRORLEVEL%
)

echo Success! Your all-in-one installer is in the current directory.
pause
