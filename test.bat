@echo off
if not exist bin\com\logpulse\LogPulseTestRunner.class (
    echo Binaries not found. Building project first...
    call build.bat
    if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
)

java -ea -cp bin com.logpulse.LogPulseTestRunner
