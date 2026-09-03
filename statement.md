# Problem Statement Document: LogPulse

**Project Name**: LogPulse — Multi-Threaded Server Log Anomaly & Rate Limiter Engine  
**Course Code / Name**: Programming in Java  
**Academic Component**: Evaluated Course Project (Flipped Course Evaluation)  
**Platform**: VITyarthi — VIT Bhopal University  
**Author**: Dhrrishit V Deka  
**Contact Email**: n9yyk6uuu@mozmail.com  

---

## 1. Problem Statement
Modern cloud web servers, microservices, reverse proxies, and API gateways generate millions of log lines per hour in heterogeneous formats (Apache Combined Log, Nginx, JSON, and Syslog). System administrators, DevOps engineers, and Site Reliability Engineers (SREs) face two critical operational challenges:
1. **Security Vulnerability to Silent Incursions**: Malicious actors execute automated credential stuffing, distributed brute-force authentication attacks (401/403 status bursts), and vulnerability endpoint reconnaissance (e.g., probing for `/.env`, `/wp-admin`, `/actuator`).
2. **Resource Exhaustion & Observability Lag**: Traditional post-mortem analysis utilities often read entire multi-gigabyte log files into memory at once, risking `OutOfMemoryError` (OOM) crashes, or depend on heavy, multi-node log indexing stacks (such as the ELK stack) which cannot easily be run locally on a developer's workstation or CI/CD container for instant deterministic validation.

**LogPulse** solves this problem by providing a lightweight, high-throughput, multi-threaded command-line log analysis engine in pure Java. It streams server logs via non-blocking bounded queues, evaluates concurrent sliding-window rate limiting and security anomaly rules in real time, tracks top offending entities using heap algorithms, and outputs structured CLI dashboards and SIEM-compliant audit logs (JSON/CSV).

---

## 2. Scope of the Project
* **In Scope**:
  * Ingestion and tokenization of high-volume web server logs in standard formats: Apache/Nginx Combined Log Format (CLF), JSON microservice log streams, and RFC 5424/3164 Syslog.
  * Multi-threaded Producer-Consumer pipeline decoupling file I/O from compute-intensive regex parsing and rule evaluation.
  * In-memory, thread-safe sliding-window rate calculation with automatic timestamp eviction ($O(1)$ amortized time complexity per entry).
  * Anomaly detection rules covering:
    * Brute-force authentication bursts (401/403 spikes per IP within a rolling time window).
    * High-frequency request floods exceeding rate-limiting thresholds (DoS / aggressive scrapers).
    * Web reconnaissance and path scanner detection (`/.env`, `/wp-admin`, `/.git`, traversal patterns).
    * Upstream backend failure surges (HTTP 5xx error bursts).
  * Algorithmic Top-K offending IP tracking using an in-memory Priority Queue (Min-Heap).
  * Formatted ANSI CLI terminal reporting and automated JSON/CSV report persistence.
  * Self-contained unit and integration testing suite executable across headless environments.
* **Out of Scope**:
  * Real-time network packet capture (pcap sniffing at the OSI network layer).
  * Distributed clustering across remote server clusters (the engine is targeted as a high-performance local/CI/CD CLI engine).
  * Graphical user interfaces (GUI/Swing/JavaFX) — strictly terminal-first and headless-compatible.

---

## 3. Target Users
1. **DevOps & Site Reliability Engineers (SREs)**: Requiring a fast CLI utility to inspect server logs during live outages or incident responses without setting up bulky monitoring infrastructure.
2. **Security Operations Center (SOC) Analysts**: Auditing web server access dumps to extract offending attacker IPs, brute-force timelines, and reconnaissance attempts into JSON/CSV for firewall rule creation.
3. **Backend Developers & CI/CD Pipelines**: Automated test verification of API gateway access logs and load-test output directly within headless Linux containers.

---

## 4. High-Level Features
* **Streaming NIO Ingestion**: Chunked buffered stream reader guaranteeing minimal heap footprint regardless of log file size ($O(1)$ memory usage relative to total file size).
* **Producer-Consumer Worker Pool**: Dynamic worker thread pool (`ExecutorService`) synchronized via bounded `BlockingQueue` and poison-pill termination tokens.
* **Pluggable Parser Strategies**: Runtime strategy selection (`LogParser`) supporting Apache Combined, JSON, and Syslog with automatic format sniffing.
* **Rolling Sliding-Window Limiter**: Thread-safe timestamp deques (`ConcurrentHashMap` + `ArrayDeque`) with active window pruning.
* **Heuristic Top-K Aggregation**: Min-Heap (`PriorityQueue`) maintaining the top-K malicious actors with zero sorting overhead on the full IP set.
* **Multi-Format Reporting**: ANSI terminal dashboard, machine-readable JSON audit logs, and spreadsheet-ready CSV exports.
