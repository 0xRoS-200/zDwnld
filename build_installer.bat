@echo off
setlocal

echo [1/2] Building Fat JAR with Maven...
call mvn clean package
if errorlevel 1 goto :mvn_fail

echo [2/2] Creating portable app bundle...

:: Kill any potential blockers
taskkill /f /im zDwnld.exe >nul 2>&1

:: Use a unique temp folder to avoid "Access Denied" from locked directories
set TEMP_BUILD=build_tmp_%RANDOM%
if exist %TEMP_BUILD% rd /s /q %TEMP_BUILD%
mkdir %TEMP_BUILD%

set JP_CMD="C:\Program Files\Java\jdk-26\bin\jpackage.exe"

echo Waiting for file system to settle...
timeout /t 3 /nobreak >nul

:run_jp
%JP_CMD% --input target/ ^
         --dest %TEMP_BUILD% ^
         --name zDwnld ^
         --main-jar zDwnld-1.0-SNAPSHOT.jar ^
         --main-class opensource.DlacInc.ZDwnld.ZDwnld ^
         --type app-image ^
         --icon Icon.ico ^
         --app-version 1.0.0 ^
         --vendor "DlacInc"

if errorlevel 1 goto :jp_fail

echo.
echo [3/3] Finalizing...
:: Try to clean up the old folder, but don't fail if it's locked
if exist zDwnld rd /s /q zDwnld >nul 2>&1
move %TEMP_BUILD%\zDwnld .\zDwnld >nul
rd /s /q %TEMP_BUILD% >nul 2>&1

echo.
echo Done! Your portable app is in the "zDwnld" folder.
echo The icon is embedded directly into "zDwnld\zDwnld.exe".
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
