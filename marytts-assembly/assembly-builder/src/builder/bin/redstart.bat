@echo off
set BINDIR=%~dp0
call :RESOLVE "%BINDIR%\.." MARY_BASE
java -showversion "%*" -cp ".;%MARY_BASE%/lib/*" marytts.tools.redstart.Redstart
goto :EOF

:RESOLVE
set %2=%~f1
goto :EOF
