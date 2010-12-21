@echo off

rem Set the Mary base installation directory in an environment variable:
set BINDIR=%~dp0

call :RESOLVE "%BINDIR%\.." MARY_BASE

set PATH=%MARY_BASE%\lib\windows;%PATH%

set CLASSPATH="%MARY_BASE%\java\mary-common.jar;%MARY_BASE%\java\log4j-1.2.15.jar"
java -showversion -ea -Xms40m -Xmx1g -cp %CLASSPATH% "-Dmary.base=%MARY_BASE%" marytts.server.Mary
goto :EOF

:RESOLVE
set %2=%~f1
goto :EOF
