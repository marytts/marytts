@echo off
set BINDIR=%~dp0
call :RESOLVE "%BINDIR%\.." MARY_BASE

java -showversion -Dserver.host=localhost -Dserver.port=59125 -jar "%MARY_BASE%\lib\marytts-client-${project.version}-jar-with-dependencies.jar"
goto :EOF

:RESOLVE
set %2=%~f1
goto :EOF
