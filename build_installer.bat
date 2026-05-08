@echo off
setlocal

echo [1/2] Building Fat JAR with Maven...
call mvn clean package
if errorlevel 1 goto :mvn_fail

echo [2/2] Creating Windows Installer (.exe)...

set JP_CMD=jpackage
where jpackage >nul 2>nul
if not errorlevel 1 goto :run_jp

if not defined JAVA_HOME goto :run_jp
if not exist "%JAVA_HOME%\bin\jpackage.exe" goto :run_jp
set JP_CMD="%JAVA_HOME%\bin\jpackage.exe"

:run_jp
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

if errorlevel 1 goto :jp_fail

echo Success! Your all-in-one installer is in the current directory.
pause
exit /b 0

:mvn_fail
echo Maven build failed.
pause
exit /b 1

:jp_fail
echo.
echo ERROR: jpackage failed or was not found.
echo 1. Ensure you have a JDK installed (not just JRE).
echo 2. Ensure you have the WiX Toolset installed: https://wixtoolset.org/
pause
exit /b 1
