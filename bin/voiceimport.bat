@echo off
set BINDIR=%~dp0%
set MARY_BASE=%BINDIR%\..
java -ea -Xmx1024m -jar "%MARY_BASE%"\java\voiceimport.jar

