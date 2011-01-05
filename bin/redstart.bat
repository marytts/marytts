@echo off
set BINDIR=%~dp0
call :RESOLVE "%BINDIR%\.." MARY_BASE
java -showversion -ea "%*" -jar "%MARY_BASE%"\java\redstart.jar
goto :EOF

:RESOLVE
set %2=%~f1
goto :EOF
