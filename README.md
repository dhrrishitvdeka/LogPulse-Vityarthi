# LogPulse: Multi-Threaded Server Log Anomaly & Rate Limiter Engine

[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2026-blue.svg)](https://www.oracle.com/java/)
[![Build](https://img.shields.io/badge/Build-Maven%20%7C%20Javac-success.svg)]()
[![Tests](https://img.shields.io/badge/Tests-100%25%20Passing-brightgreen.svg)]()
[![CLI Executable](https://img.shields.io/badge/Interface-CLI%20Terminal-orange.svg)]()

> A high-throughput, concurrent command-line log analysis and real-time security anomaly detection engine built in Java. It streams server logs via bounded queues, evaluates sliding-window rate limits and security intrusion rules across concurrent worker threads, and generates rich terminal dashboards and SIEM-ready audit reports (JSON/CSV).

---

## 1. Overview
Modern internet infrastructure generates continuous streams of HTTP access logs from reverse proxies (Nginx, HAProxy), application servers (Tomcat, Spring Boot), and microservices. Detecting malicious traffic patterns—such as distributed credential brute-forcing, high-frequency web scraping, or reconnaissance probes—typically requires bulky, resource-heavy enterprise stacks.

**LogPulse** delivers a self-contained, memory-bounded, multi-threaded CLI utility written in pure Java SE. Designed for deterministic execution in terminal environments and CI/CD pipelines, LogPulse utilizes a **Producer-Consumer architecture** to decouple file streaming I/O from concurrent regex tokenization, sliding-window rate tracking, and heap-based Top-K offense aggregation.

---

## 2. Key Features

* **Multi-Threaded Producer-Consumer Pipeline**: Employs an `ArrayBlockingQueue` with an `ExecutorService` thread pool to maximize CPU core utilization without exhausting heap memory on multi-gigabyte log archives.
* **Pluggable Parser Strategies (Strategy Pattern)**: Auto-detects and parses multiple standard log representations:
  * Apache & Nginx Combined Log Format (CLF)
  * Cloud/Microservice JSON logs (`ndjson`)
  * RFC 5424 / RFC 3164 Syslog streams
* **High-Performance Sliding-Window Engine**: Implements in-memory timestamp deques (`ArrayDeque`) with lock striping to detect event rate bursts within rolling $T$-second time windows in amortized $O(1)$ time.
* **Multi-Vector Anomaly Detection Rules**:
  * **Brute-Force Authentication Detection**: Alerts on threshold spikes of HTTP 401/403 responses per IP within a sliding window.
  * **DoS / Rate-Limit Violations**: Identifies IP addresses exceeding configured request frequency limits.
  * **Reconnaissance & Path Scanning**: Intercepts queries targeting sensitive endpoints (`/wp-admin`, `/.env`, `/.git`, directory traversal `../`).
  * **Server Error Surges**: Detects cascading upstream application crashes (HTTP 5xx spikes).
* **Algorithmic Top-K Tracking (Min-Heap)**: Leverages Java's `PriorityQueue` to maintain and rank the highest offending IP addresses in $O(N \log K)$ time.
* **Rich Terminal Dashboard & Export Subsystem**: Features ANSI-styled status summaries and supports one-flag exports to structured JSON and CSV formats.
* **100% Headless & CLI Deterministic**: Zero GUI/display dependencies; exits with standard exit codes (`0` for success, `1` for error), making it ideal for automated grading and Docker containers.

---

## 3. Technologies & Architecture

* **Language**: Java 17+ (Fully tested on modern OpenJDK / Oracle JDK).
* **Core APIs**:
  * `java.util.concurrent` (`ArrayBlockingQueue`, `ExecutorService`, `CountDownLatch`, `ConcurrentHashMap`, `AtomicLong`)
  * `java.nio.file` (Non-blocking buffered stream processing)
  * `java.time` (`Instant`, `Duration`, `ZonedDateTime`)
  * `java.util` (`PriorityQueue`, `ArrayDeque`, `Optional`)
* **Design Patterns**: Strategy Pattern, Factory Pattern, Producer-Consumer Pattern, DTO/Record Pattern.
* **Build Systems**: Direct `javac` (Zero external runtime dependencies) & Maven (`pom.xml`) with JUnit 5.

---

## 4. Installation & Setup

### Prerequisites
* **Java Development Kit (JDK)**: Version 17 or higher (`javac` and `java` available on PATH).
* **Optional**: Apache Maven 3.8+ (if building via Maven).

Check your installed Java version:
```bash
java -version
javac -version
```

### Cloning the Repository
```bash
git clone https://github.com/{your-username}/Java-VitYarthi.git
cd Java-VitYarthi
```

---

## 5. How to Build and Run (Command-Line)

### Method A: Direct Compilation via Javac (Recommended & Simplest)

#### On Windows:
```cmd
:: 1. Compile the project
build.bat

:: 2. Run the test suite
test.bat

:: 3. Execute the CLI with sample attack log
run.bat
```

#### On Linux / macOS:
```bash
# 1. Make scripts executable
chmod +x run.sh

# 2. Compile directly using javac
mkdir -p bin
javac -d bin $(find src -name "*.java")

# 3. Run the automated test runner
java -ea -cp bin com.logpulse.LogPulseTestRunner

# 4. Execute the application
./run.sh
```

---

### Method B: Building with Maven
```bash
# Compile and run unit tests
mvn clean test

# Package executable JAR
mvn package

# Run the packaged JAR
java -jar target/logpulse-engine-1.0.0.jar --file sample_logs/brute_force_attack.log --export json --output target/report.json
```

---

## 6. CLI Usage & Options

```
java -cp bin com.logpulse.Main --file <path> [options]
```

### Command-Line Arguments Table

| Flag | Short | Description | Default |
| :--- | :---: | :--- | :--- |
| `--file` | `-f` | **(Required)** Path to the input server log file. | *None* |
| `--format` | | Parser strategy: `auto`, `apache`, `json`, `syslog` | `auto` |
| `--window` | `-w` | Sliding-window duration in seconds. | `60` |
| `--rate-limit`| `-r` | Maximum requests per IP in the window before rate alert. | `50` |
| `--auth-threshold`| `-a` | Consecutive 401/403 failures triggering brute-force alert. | `5` |
| `--error-threshold`| `-e` | HTTP 5xx errors triggering server burst alert. | `10` |
| `--top` | `-k` | Number of top offending IPs to display in the ranking table. | `5` |
| `--threads` | `-t` | Number of parallel worker threads in the consumer pool. | CPU Cores |
| `--export` | | Export format: `none`, `json`, `csv`, `all` | `none` |
| `--output` | `-o` | Output file path for audit reports. | `report.json` |
| `--help` | `-h` | Prints usage manual and exits. | |

### Example Commands

#### 1. Baseline Web Traffic Analysis
```bash
java -cp bin com.logpulse.Main --file sample_logs/web_traffic.log
```

#### 2. Brute-Force & Vulnerability Scan Detection with JSON Export
```bash
java -cp bin com.logpulse.Main --file sample_logs/brute_force_attack.log --auth-threshold 4 --window 60 --export json --output target/brute_force_report.json
```

#### 3. DoS Rate-Limit Burst Analysis with CSV Export
```bash
java -cp bin com.logpulse.Main --file sample_logs/rate_limit_burst.log --rate-limit 10 --window 60 --export csv --output target/rate_limit_report.csv
```

#### 4. Cloud Microservice JSON Log Inspection
```bash
java -cp bin com.logpulse.Main --file sample_logs/microservice.json.log --format json --error-threshold 3 --export all --output target/microservice_report
```

---

## 7. Instructions for Testing

LogPulse includes a dedicated, self-contained automated verification suite with assertion checks covering parser fidelity, sliding-window eviction, concurrency correctness, and Top-K heap ordering.

Run tests using the CLI runner:
```bash
# Windows
test.bat

# Linux / macOS / Terminal
java -ea -cp bin com.logpulse.LogPulseTestRunner
```

Expected Test Output:
```
================================================================================
LOGPULSE TEST SUITE: RUNNING AUTOMATED UNIT & INTEGRATION VERIFICATION
================================================================================

[1/4] Running LogParser Tests...
  ✔ ParserTest: All parser test suites passed.

[2/4] Running SlidingWindow Rate Limiter & Concurrency Tests...
  ✔ SlidingWindowTest: All sliding-window test suites passed.

[3/4] Running Anomaly Detection Rule Tests...
  ✔ AnomalyDetectionTest: All detection rule test suites passed.

[4/4] Running Pipeline Concurrency & Top-K Heap Tests...
  ✔ PipelineConcurrencyTest: Aggregation & concurrency tests passed.

================================================================================
ALL TEST SUITES PASSED (4/4) in 120 ms.
Exit Code: 0 (OK)
================================================================================
```

---

## 8. Sample Terminal Execution & Results

```
Initializing LogPulse Engine on: sample_logs/brute_force_attack.log...
Configuration: Worker Threads = 8 | Format = auto | Window = 60s
================================================================================
               LOGPULSE // HIGH-THROUGHPUT ANOMALY ENGINE                       
================================================================================

[ 1. PIPELINE TELEMETRY & PERFORMANCE ]
  Total Processing Time    : 0.091 seconds
  Total Lines Processed    : 18 lines (0.00 MB)
  Valid Lines Parsed       : 18
  Malformed / Skipped      : 0
  Throughput               : 198 lines/sec (0.02 MB/sec)

[ 2. HTTP STATUS DISTRIBUTION ]
  2xx Success: 2 | 3xx Redirect: 0 | 4xx Client Error: 16 | 5xx Server Error: 0

[ 3. DETECTED ANOMALY BREAKDOWN ]
  Total Incidents Flagged: 5
  - BRUTE_FORCE_AUTH                : 2 incident(s)
  - SUSPICIOUS_PATH_SCAN            : 3 incident(s)

[ 4. TOP 5 OFFENDING IP ADDRESSES ]
  +----+-----------------+------------+---------------+----------------------------------+
  | #  | IP Address      | Incidents  | Max Severity  | Primary Anomaly Vector           |
  +----+-----------------+------------+---------------+----------------------------------+
  | 1  | 198.51.100.99   | 3          | HIGH          | SUSPICIOUS_PATH_SCAN             |
  | 2  | 203.0.113.45    | 1          | HIGH          | BRUTE_FORCE_AUTH                 |
  | 3  | 198.51.100.22   | 1          | HIGH          | BRUTE_FORCE_AUTH                 |
  +----+-----------------+------------+---------------+----------------------------------+

[ 5. RECENT CRITICAL INCIDENTS AUDIT TRAIL ]
  [HIGH] SUSPICIOUS_PATH_SCAN 198.51.100.99   -> Reconnaissance signature '/.env' detected in request URL '/.env'
  [HIGH] SUSPICIOUS_PATH_SCAN 198.51.100.99   -> Reconnaissance signature '/phpmyadmin' detected in request URL '/phpmyadmin/index.php'
  [HIGH] BRUTE_FORCE_AUTH 203.0.113.45    -> Observed 4 consecutive 401/403 failures on '/api/v1/auth/login' within 60s window
  [HIGH] BRUTE_FORCE_AUTH 198.51.100.22   -> Observed 4 consecutive 401/403 failures on '/admin/auth' within 60s window
  [HIGH] SUSPICIOUS_PATH_SCAN 198.51.100.99   -> Reconnaissance signature '/wp-admin' detected in request URL '/wp-admin/login.php'

================================================================================
LogPulse Execution Completed Successfully. Exit Code: 0
================================================================================
✔ Exported JSON audit report to: target/brute_force_report.json
```

---

## 9. Repository Structure
```
Java-VitYarthi/
├── pom.xml                                 # Maven configuration with JUnit 5
├── README.md                               # Project documentation & run guide
├── statement.md                            # Academic problem statement
├── PROJECT_REPORT.md                       # Full 15-section project report (PDF exportable)
├── build.bat                               # Windows one-click compile script
├── run.bat                                 # Windows one-click run script
├── test.bat                                # Windows one-click test script
├── run.sh                                  # Unix/Linux one-click execution script
├── sample_logs/                            # Synthetic test data files
│   ├── web_traffic.log                     # Standard baseline HTTP traffic
│   ├── brute_force_attack.log              # Attack log with 401/403 credential bursts
│   ├── rate_limit_burst.log                # Rapid scraping/DoS request flood
│   └── microservice.json.log               # JSON-formatted microservice log stream
└── src/
    ├── main/java/com/logpulse/
    │   ├── Main.java                       # CLI entrypoint & argument parser
    │   ├── config/LogPulseConfig.java      # Configuration container & builder
    │   ├── model/                          # Domain entities (LogEntry, Incident, etc.)
    │   ├── parser/                         # Strategy parsers (Apache, JSON, Syslog, Factory)
    │   ├── engine/                         # Producer-Consumer pipeline, sliding window, rules
    │   ├── aggregator/                     # Thread-safe aggregator & Top-K Min-Heap
    │   ├── reporter/                       # Terminal dashboard & JSON/CSV report exporters
    │   └── exception/                      # Domain-specific exception hierarchy
    └── test/java/com/logpulse/             # Automated test runner and unit test suites
```

---

## 10. Author & Contact

* **Author**: Dhrrishit V Deka
* **Email**: [n9yyk6uuu@mozmail.com](mailto:n9yyk6uuu@mozmail.com)
* **Institution**: VIT Bhopal University — School of Computing Science and Engineering
* **Course**: Programming in Java (Evaluated Course Project, VITyarthi Platform)
