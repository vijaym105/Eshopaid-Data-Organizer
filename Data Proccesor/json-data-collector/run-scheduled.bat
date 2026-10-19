@echo off
setlocal

REM Windows Task Scheduler entry point.
REM Configure EXTERNAL_API_URL and EXTERNAL_API_TOKEN as Windows
REM environment variables before running this file.

cd /d "%~dp0"

if "%EXTERNAL_API_URL%"=="" (
    echo ERROR: EXTERNAL_API_URL is not set.
    exit /b 1
)

if "%EXTERNAL_API_TOKEN%"=="" (
    echo ERROR: EXTERNAL_API_TOKEN is not set.
    exit /b 1
)

java -jar "target\demo-0.0.1-SNAPSHOT.jar" --run-once
exit /b %ERRORLEVEL%
