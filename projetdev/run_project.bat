@echo off
setlocal
cd /d %~dp0

:MENU
cls
echo ====================================================
echo        PIDEV Project Launcher
echo ====================================================
echo 1. Run Full Application (Login)
echo 2. Run Front Office (Candidate Dashboard)
echo 3. Run Back Office (Recruiter Dashboard)
echo 4. Rebuild Project (Maven Clean Compile)
echo 5. Exit
echo ====================================================
set /p "choice=Enter your choice (1-5): "

if "%choice%"=="1" goto RUN_MAIN
if "%choice%"=="2" goto RUN_FRONT
if "%choice%"=="3" goto RUN_BACK
if "%choice%"=="4" goto BUILD
if "%choice%"=="5" goto EXIT

echo Invalid choice. Please try again.
timeout /t 2 >nul
goto MENU

:RUN_MAIN
cls
echo Starting Full Application...
call mvn exec:java -Dexec.mainClass="Application.MainApp"
pause
goto MENU

:RUN_FRONT
cls
echo Starting Front Office...
call mvn exec:java -Dexec.mainClass="Application.MainAppFrontOffice"
pause
goto MENU

:RUN_BACK
cls
echo Starting Back Office...
call mvn exec:java -Dexec.mainClass="Application.MainAppBackOffice"
pause
goto MENU

:BUILD
cls
echo Rebuilding Project...
call mvn clean compile
if %ERRORLEVEL% EQU 0 (
    echo Build Successful!
) else (
    echo Build Failed!
)
pause
goto MENU

:EXIT
endlocal
exit
