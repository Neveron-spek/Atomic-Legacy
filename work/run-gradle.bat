@echo off
rem Runs Gradle offline for NeoNTM. Tasks are read from work\cu-tasks.txt (one line).
setlocal
cd /d "%~dp0.."
set "LOG=work\cu-run.log"
set "G="
for /f "delims=" %%i in ('where gradle 2^>nul') do if not defined G set "G=%%i"
if not defined G for /f "delims=" %%i in ('dir /s /b "%USERPROFILE%\.gradle\wrapper\dists\gradle.bat" 2^>nul') do if not defined G set "G=%%i"
if not defined G for /f "delims=" %%i in ('dir /s /b "%USERPROFILE%\Documents\Codex\gradle.bat" 2^>nul') do if not defined G set "G=%%i"
set /p TASKS=<work\cu-tasks.txt
> "%LOG%" echo GRADLE=%G%
>> "%LOG%" echo TASKS=%TASKS%
>> "%LOG%" echo JAVA_HOME=%JAVA_HOME%
java -version >> "%LOG%" 2>&1
if not defined G (
  >> "%LOG%" echo CU_EXIT=NOGRADLE
  exit /b 1
)
call "%G%" --offline --no-daemon --gradle-user-home work\gradle-cache %TASKS% >> "%LOG%" 2>&1
>> "%LOG%" echo CU_EXIT=%ERRORLEVEL%
