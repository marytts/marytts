@echo off
set BINDIR=%~dp0
call :RESOLVE "%BINDIR%\.." MARY_BASE
java -showversion -ea -Xmx1024m -DMARYBASE="%MARY_BASE%" "%*" -jar "%MARY_BASE%\lib\marytts-builder-${project.version}-jar-with-dependencies.jar"
goto :EOF

:RESOLVE
set %2=%~f1
goto :EOF
