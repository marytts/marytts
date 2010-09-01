@echo off
set BINDIR=%~dp0%
set MARY_BASE=%BINDIR%\..

set CLASSPATH="%MARY_BASE%\java\maryclient.jar;%MARY_BASE%\java\java-diff.jar;%MARY_BASE%\java\log4j-1.2.15.jar"

java -ea -cp %CLASSPATH% -Dserver.host=localhost marytts.client.MaryGUIClient
