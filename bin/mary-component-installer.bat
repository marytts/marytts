@echo off
set BINDIR=%~dp0%
set MARY_BASE=%BINDIR%\..
java -ea -Dmary.base="%MARY_BASE%" -jar "%MARY_BASE%"\java\mary-component-installer.jar

