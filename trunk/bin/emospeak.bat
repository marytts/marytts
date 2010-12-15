@echo off
set BINDIR=%~dp0
call :RESOLVE "%BINDIR%\.." MARY_BASE
java -ea -Dserver.host=localhost -jar "%MARY_BASE%"\java\emospeak.jar
goto :EOF

:RESOLVE
set %2=%~f1
goto :EOF
