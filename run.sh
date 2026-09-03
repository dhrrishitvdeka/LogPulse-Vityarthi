#!/usr/bin/env bash
set -e

# Build if binaries do not exist
if [ ! -f "bin/com/logpulse/Main.class" ]; then
    echo "Compiling LogPulse Engine..."
    mkdir -p bin
    javac -d bin $(find src -name "*.java")
fi

if [ -z "$1" ]; then
    java -cp bin com.logpulse.Main --file sample_logs/brute_force_attack.log --export json --output target/report.json
else
    java -cp bin com.logpulse.Main "$@"
fi
