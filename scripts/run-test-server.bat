@echo off
rem Starts a local dedicated test server with the current Atomic Legacy sources (folder run-server).
rem Development server: it does not ask for eula.txt, but the Minecraft EULA (https://aka.ms/MinecraftEULA) still applies.
cd /d "%~dp0.."
call "%USERPROFILE%\.gradle\wrapper\dists\gradle-8.12.1-bin\eumc4uhoysa37zql93vfjkxy0\gradle-8.12.1\bin\gradle.bat" --offline --no-daemon --gradle-user-home "%CD%\work\gradle-cache" runServer
pause
