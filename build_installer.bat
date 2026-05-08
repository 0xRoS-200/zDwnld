@echo off
setlocal

echo [1/2] Building Fat JAR with Maven...
call mvn clean package
if errorlevel 1 goto :mvn_fail

echo [2/2] Creating portable app bundle...

:: Kill any running instances to prevent file locks
taskkill /f /im zDwnld.exe >nul 2>&1
if exist zDwnld rd /s /q zDwnld

set JP_CMD="C:\Program Files\Java\jdk-26\bin\jpackage.exe"

:run_jp
timeout /t 3 /nobreak >nul
%JP_CMD% --input target/ ^
         --name zDwnld ^
         --main-jar zDwnld-1.0-SNAPSHOT.jar ^
         --main-class opensource.DlacInc.ZDwnld.ZDwnld ^
         --type app-image ^
         --icon Icon.ico ^
         --app-version 1.0.0 ^
         --vendor "DlacInc"

if errorlevel 1 goto :jp_fail

echo.
echo Done! Your portable app is in the "zDwnld" folder.
echo The icon is embedded directly into "zDwnld\zDwnld.exe".
echo You can zip the zDwnld folder and share it with anyone!
pause
exit /b 0

:mvn_fail
echo Maven build failed.
pause
exit /b 1

:jp_fail
echo.
echo ERROR: jpackage failed or was not found.
echo Make sure you have a JDK installed (not just JRE) and JAVA_HOME is set.
pause
exit /b 1
