@echo off

rem Set the Mary base installation directory in an environment variable:
set BINDIR=%~dp0%

set MARY_BASE=%BINDIR%\..

set PATH=%MARY_BASE%\lib\windows;%PATH%

set CLASSPATH="%MARY_BASE%\java\mary-common.jar;%MARY_BASE%\java\log4j-1.2.15.jar"
java -ea -Xms40m -Xmx512m -cp %CLASSPATH% "-Dmary.base=%MARY_BASE%" marytts.server.Mary

