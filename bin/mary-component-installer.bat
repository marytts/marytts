@echo off
set BINDIR=%~dp0
call :RESOLVE "%BINDIR%\.." MARY_BASE
java -showversion -ea -Dmary.base="%MARY_BASE%" -jar "%MARY_BASE%"\java\mary-component-installer.jar
goto :EOF

:RESOLVE
set %2=%~f1
goto :EOF
