@echo off

rem Set the Mary base installation directory in an environment variable:
set BINDIR=%~dp0

call :RESOLVE "%BINDIR%\.." MARY_BASE

set CLASSPATH=".;%MARY_BASE%\lib\*"
java -showversion -ea -Xms40m -Xmx1g -cp %CLASSPATH% "-Dmary.base=%MARY_BASE%" marytts.server.Mary
goto :EOF

:RESOLVE
set %2=%~f1
goto :EOF
