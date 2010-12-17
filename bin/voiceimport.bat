@echo off
set BINDIR=%~dp0
call :RESOLVE "%BINDIR%\.." MARY_BASE
java -ea -Xmx1024m -DMARYBASE="%MARY_BASE%" "%*" -jar "%MARY_BASE%"\java\voiceimport.jar
goto :EOF

:RESOLVE
set %2=%~f1
goto :EOF
