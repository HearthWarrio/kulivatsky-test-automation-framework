@echo off
setlocal
set "BASE_DIR=%~dp0"
set "PROPS=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"

for /F "usebackq tokens=1,* delims==" %%A in ("%PROPS%") do (
  if "%%A"=="mavenVersion" set "MAVEN_VERSION=%%B"
  if "%%A"=="distributionUrl" set "DIST_URL=%%B"
  if "%%A"=="distributionSha512Sum" set "DIST_SHA512=%%B"
)

for %%I in ("%DIST_URL%") do set "DIST_FILE=%%~nxI"
if defined MAVEN_USER_HOME (
  set "CACHE_ROOT=%MAVEN_USER_HOME%\wrapper\dists\ktaf"
) else (
  set "CACHE_ROOT=%USERPROFILE%\.m2\wrapper\dists\ktaf"
)
set "MAVEN_HOME=%CACHE_ROOT%\apache-maven-%MAVEN_VERSION%"
set "ARCHIVE=%CACHE_ROOT%\%DIST_FILE%"

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto runMaven

if not exist "%CACHE_ROOT%" mkdir "%CACHE_ROOT%"
set "TEMP_ARCHIVE=%ARCHIVE%.tmp"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "Invoke-WebRequest -UseBasicParsing -Uri '%DIST_URL%' -OutFile '%TEMP_ARCHIVE%';" ^
  "$hash=(Get-FileHash -Algorithm SHA512 '%TEMP_ARCHIVE%').Hash.ToLowerInvariant();" ^
  "if ($hash -ne '%DIST_SHA512%') { throw 'Maven distribution checksum mismatch.' };" ^
  "if (Test-Path '%MAVEN_HOME%') { Remove-Item -Recurse -Force '%MAVEN_HOME%' };" ^
  "Expand-Archive -Force '%TEMP_ARCHIVE%' '%CACHE_ROOT%';" ^
  "Move-Item -Force '%TEMP_ARCHIVE%' '%ARCHIVE%'"
if errorlevel 1 exit /b %errorlevel%

:runMaven
call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %errorlevel%
