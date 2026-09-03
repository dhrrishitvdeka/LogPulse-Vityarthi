@echo off
echo ================================================================================
echo Building LogPulse Engine (Java SE Compiler)
echo ================================================================================

if not exist bin mkdir bin

javac -d bin src\main\java\com\logpulse\*.java ^
              src\main\java\com\logpulse\config\*.java ^
              src\main\java\com\logpulse\model\*.java ^
              src\main\java\com\logpulse\parser\*.java ^
              src\main\java\com\logpulse\engine\*.java ^
              src\main\java\com\logpulse\engine\rules\*.java ^
              src\main\java\com\logpulse\aggregator\*.java ^
              src\main\java\com\logpulse\reporter\*.java ^
              src\main\java\com\logpulse\exception\*.java ^
              src\test\java\com\logpulse\*.java

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Compilation completed successfully. Output directory: bin/
) else (
    echo [ERROR] Compilation failed.
    exit /b %ERRORLEVEL%
)
