@echo off
setlocal
set "BASE_DIR=%~dp0"
set "PROPS=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"
set "BOOTSTRAP=%BASE_DIR%.mvn\wrapper\MavenBootstrap.java"

if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java"
)

"%JAVA_EXE%" -version >nul 2>&1
if errorlevel 1 (
  echo Error: Java 21 or newer is required to run the Maven wrapper. 1>&2
  exit /b 1
)

if defined MAVEN_USER_HOME (
  set "CACHE_ROOT=%MAVEN_USER_HOME%\wrapper\dists\ktaf"
) else (
  set "CACHE_ROOT=%USERPROFILE%\.m2\wrapper\dists\ktaf"
)

set "BOOTSTRAP_OUT=%TEMP%\ktaf-mvnw-%RANDOM%-%RANDOM%.txt"
"%JAVA_EXE%" "%BOOTSTRAP%" "%PROPS%" "%CACHE_ROOT%" > "%BOOTSTRAP_OUT%"
if errorlevel 1 (
  del /q "%BOOTSTRAP_OUT%" >nul 2>&1
  exit /b 1
)
set /p "MAVEN_HOME="<"%BOOTSTRAP_OUT%"
del /q "%BOOTSTRAP_OUT%" >nul 2>&1

if not defined MAVEN_HOME (
  echo Error: Maven bootstrap did not return an installation directory. 1>&2
  exit /b 1
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %errorlevel%
