@echo off
setlocal

set "ROOT=%~dp0"
set "JAVA_HOME=%ROOT%.tools\jdk\jdk-21.0.11+10"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Local JDK not found: %JAVA_HOME%
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%ROOT%server"
call mvnw.cmd -q -DskipTests compile
if errorlevel 1 exit /b 1

call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo
