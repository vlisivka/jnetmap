@echo off
setlocal

rem We use the value the JAVACMD environment variable, if defined
rem and then try JAVA_HOME
set "_JAVACMD=%JAVACMD%"
if "%_JAVACMD"=="" (
  if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" (
      set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
      set "_JAVAWCMD=%JAVA_HOME%\bin\java.exe"
    )
  )
)
if "%_JAVACMD%"=="" (
  set _JAVACMD=java
  set _JAVAWCMD=javaw
)

rem Parses x out of 1.x; for example 8 out of java version 1.8.0_xx
rem Otherwise, parses the major version; 9 out of java version 9-ea
set JAVA_VERSION=0
for /f "tokens=3" %%g in ('%_JAVACMD% -Xms32M -Xmx32M -version 2^>^&1 ^| findstr /i "version"') do (
  set JAVA_VERSION=%%g
)
set JAVA_VERSION=%JAVA_VERSION:"=%
for /f "delims=.-_ tokens=1-2" %%v in ("%JAVA_VERSION%") do (
  if /I "%%v" EQU "1" (
    set JAVA_VERSION=%%w
  ) else (
    set JAVA_VERSION=%%v
  )
)

rem @echo Java %JAVA_VERSION%

cd /d %~dp0%
if %JAVA_VERSION% EQU 8 (
  start %_JAVAWCMD% -jar jNetMap.jar %*
) else (
  if %JAVA_VERSION% GTR 8 (
    start %_JAVAWCMD% --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.desktop/java.awt.geom=ALL-UNNAMED --add-opens=java.desktop/javax.swing.event=ALL-UNNAMED --add-opens=java.desktop/com.sun.java.swing.plaf.motif=ALL-UNNAMED --add-opens=java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED -jar jNetMap.jar %*
  ) else (
    @echo Requires Java 8 or later to be installed
  )
)

endlocal
