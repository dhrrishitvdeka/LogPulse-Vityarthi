@echo off
if not exist bin\com\logpulse\Main.class (
    echo Binaries not found. Building project first...
    call build.bat
    if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
)

if "%~1"=="" (
    java -cp bin com.logpulse.Main --file sample_logs/brute_force_attack.log --export json --output target/report.json
) else (
    java -cp bin com.logpulse.Main %*
)
