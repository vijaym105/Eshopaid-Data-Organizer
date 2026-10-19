@echo off

cd /d "C:\Data Collector\API-Authentication"

echo ========================================
echo eShopaid Authentication Job
echo Started: %date% %time%
echo ========================================

java -jar "api-authentication-1.0.0.jar"

echo ========================================
echo Finished: %date% %time%
echo Exit Code: %ERRORLEVEL%
echo ========================================

exit /b %ERRORLEVEL%