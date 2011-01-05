@echo off
set BINDIR=%~dp0
call :RESOLVE "%BINDIR%\.." MARY_BASE

set CLASSPATH="%MARY_BASE%\java\maryclient.jar;%MARY_BASE%\java\java-diff.jar;%MARY_BASE%\java\log4j-1.2.15.jar"

java -showversion -ea -cp %CLASSPATH% -Dserver.host=localhost marytts.client.MaryGUIClient
goto :EOF

:RESOLVE
set %2=%~f1
goto :EOF
