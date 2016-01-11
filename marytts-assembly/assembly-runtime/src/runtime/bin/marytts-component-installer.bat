@echo off
set BINDIR=%~dp0
call :RESOLVE "%BINDIR%\.." MARY_BASE
java -showversion -Dmary.base="%MARY_BASE%" -cp ".;%MARY_BASE%\lib\*" marytts.tools.install.InstallerGUI
goto :EOF

:RESOLVE
set %2=%~f1
goto :EOF
