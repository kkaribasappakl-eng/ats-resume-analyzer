@echo off
@setlocal

set ERROR_CODE=0

set "MAVEN_PROJECTBASEDIR=%~dp0"
:find_base
if exist "%MAVEN_PROJECTBASEDIR%\.mvn" goto found_base
set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR%..\"
if "%MAVEN_PROJECTBASEDIR%"=="..\" goto default_base
goto find_base
:default_base
set "MAVEN_PROJECTBASEDIR=%CD%"
:found_base

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"

if exist "%WRAPPER_JAR%" goto run_wrapper

echo Downloading Maven Wrapper...
if not exist "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper" mkdir "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper"
set "DOWNLOAD_URL="
for /f "tokens=2 delims==" %%i in ('findstr "wrapperUrl" "%WRAPPER_PROPERTIES%"') do set "DOWNLOAD_URL=%%i"
if "%DOWNLOAD_URL%"=="" (
    set "DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
)

powershell -Command "[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%WRAPPER_JAR%'"
if %ERRORLEVEL% neq 0 (
    echo Error downloading maven-wrapper.jar
    exit /b %ERRORLEVEL%
)

:run_wrapper
if "%JAVA_HOME%"=="" (
    set "JAVACMD=java"
) else (
    set "JAVACMD=%JAVA_HOME%\bin\java"
)

"%JAVACMD%" -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Java command not found. Please install Java and set JAVA_HOME.
    exit /b 1
)

"%JAVACMD%" %MAVEN_OPTS% -jar "%WRAPPER_JAR%" %*
if %ERRORLEVEL% neq 0 (
    exit /b %ERRORLEVEL%
)

@endlocal
