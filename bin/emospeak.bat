@echo off
set BINDIR=%~dp0%
set MARY_BASE=%BINDIR%..
java -ea -Dserver.host=localhost -jar "%MARY_BASE%"\java\emospeak.jar

