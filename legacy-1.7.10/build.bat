@echo off
setlocal EnableExtensions

set "ROOT=%~dp0"
set "BUILD=%ROOT%build"
set "CLASSES=%BUILD%\classes"
set "STAGE=%BUILD%\jar"
set "SOURCES=%BUILD%\sources.txt"
set "OUTPUT_ZIP=%BUILD%\RenderScale-Legacy-1.7.10-1.0.1.zip"
set "OUTPUT_JAR=%BUILD%\RenderScale-Legacy-1.7.10-1.0.1.jar"
set "CLASSPATH=%ROOT%deps\forge-universal.jar;%ROOT%deps\launchwrapper.jar;%ROOT%deps\asm-all.jar;%ROOT%deps\lwjgl.jar"

if exist "%BUILD%" rmdir /S /Q "%BUILD%"
mkdir "%CLASSES%" || exit /B 1
mkdir "%STAGE%\META-INF" || exit /B 1

for /R "%ROOT%src\main\java" %%F in (*.java) do echo "%%F">>"%SOURCES%"
if not exist "%SOURCES%" (
    echo No Java sources found.
    exit /B 1
)

java -jar "%ROOT%deps\ecj.jar" ^
    -1.7 ^
    -proc:none ^
    -d "%CLASSES%" ^
    -classpath "%CLASSPATH%" ^
    @"%SOURCES%"
if errorlevel 1 exit /B %ERRORLEVEL%

xcopy "%CLASSES%\*" "%STAGE%\" /E /I /Y >nul || exit /B 1
copy /Y "%ROOT%src\main\resources\mcmod.info" "%STAGE%\mcmod.info" >nul || exit /B 1
copy /Y "%ROOT%META-INF\MANIFEST.MF" "%STAGE%\META-INF\MANIFEST.MF" >nul || exit /B 1
copy /Y "%ROOT%LICENSE" "%STAGE%\LICENSE" >nul || exit /B 1
copy /Y "%ROOT%README.md" "%STAGE%\README.md" >nul || exit /B 1

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
    "Compress-Archive -Path '%STAGE%\*' -DestinationPath '%OUTPUT_ZIP%' -Force"
if errorlevel 1 exit /B %ERRORLEVEL%
move /Y "%OUTPUT_ZIP%" "%OUTPUT_JAR%" >nul || exit /B 1

echo Built: %OUTPUT_JAR%
exit /B 0
