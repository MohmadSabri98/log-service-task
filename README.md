# Parallel Log Risk Scanner

## 📌 Overview

This project implements a **parallel log analysis system** that scans `.log` files for potentially risky patterns.

It combines:

* **Java** → orchestration, concurrency, aggregation
* **Python** → fast, flexible pattern detection using regex
* **JSON (stdin/stdout)** → communication between Java and Python

The system is designed to be **scalable, fault-tolerant, and easy to extend**.

---

# 🚀 How to Run

## 1. Prerequisites

* Java 11 or higher
* Python 3.8 or higher
* Jackson Databind (for JSON in Java)

---

## 2. Project Structure

```
project/
 ├── input/
 │    ├── access.log
 │    └── app.log
 │
 ├── output/
 │
 ├── java/
 ├── python/
 │    └── analyzer.py
```

---

## 3. Prepare Input

Place your `.log` files inside:

```
input/
```

Each file must contain:

* One log event per line
* Plain text format

---

## 4. Compile Java

```
javac -cp ".:jackson-databind.jar" java/*.java
```

---

## 5. Run the Application

```
java -cp ".:jackson-databind.jar" java.Main
```

---

## 6. Output

After execution, results will be generated in:

```
output/
```

### Files

#### `report.json`

Contains aggregated statistics:

```json
{
  "total_flagged": 3,
  "counts_by_rule": {
    "CMD_CHAINING": 2,
    "SECRETS": 1
  }
}
```

---

#### `flagged.jsonl`

Each line is a JSON object:

```json
{"file":"app.log","line":1,"rule":"CMD_CHAINING","excerpt":"... || rm -rf /"}
```

---

# ⚙️ Assumptions

* Logs are **line-based** (one event per line)
* Input size can be large → requires chunking
* Python execution is available via `python3`
* Each chunk can be processed independently
* Regex-based detection is sufficient for this task
* Some **false positives are acceptable** (e.g. `&` in titles)
* File ordering is not critical for correctness

---

# 🧠 Design Choices

## 1. Chunk-Based Processing

* Log files are split into fixed-size chunks (e.g., 200–1000 lines)

**Benefits:**

* Enables parallelism
* Improves memory usage
* Isolates failures per chunk

---

## 2. Java for Concurrency

* Uses `ExecutorService` with a **fixed thread pool**

**Why:**

* Controlled resource usage
* Avoids thread explosion
* Predictable performance

---

## 3. Python for Analysis

* Regex detection implemented in Python

**Why:**

* Faster development
* Simpler text processing
* Easier extensibility

---

## 4. Java ↔ Python Communication

* Uses **stdin/stdout with JSON**

**Why:**

* Language-agnostic
* No network overhead
* Easy debugging and testing

---

## 5. Process Isolation

* Each chunk runs in a **separate Python process**

**Benefits:**

* Fault isolation
* No shared state issues
* Easier error handling

---

## 6. Timeout Handling

Each Python process is limited using:

```
process.waitFor(5, TimeUnit.SECONDS)
```

**Prevents:**

* Hanging processes
* Thread pool starvation

---

## 7. Aggregation Strategy

Java aggregates:

* Total flagged lines
* Counts per rule
* Per-file statistics

**Output formats:**

* JSON (summary)
* JSONL (detailed findings)

---

## 8. Fault Tolerance

* Failures are handled at **chunk level**

If a chunk fails:

* It is logged
* Processing continues

**Result:** system remains robust and does not crash بالكامل

---

# ⚠️ Limitations

* Regex-based detection (no semantic understanding)
* Possible false positives
* One Python process per chunk (can be optimized)

---

# 🚀 Future Improvements

* Add retry mechanism for failed chunks
* Replace regex with ML/NLP model
* Batch multiple chunks per Python process
* Add CLI parameters (`--threads`, `--chunk-size`)
* Add severity scoring
* Add unit tests

---

# 👤 Author

**Mohamed Sabri**
Software Engineer | Backend & AI Enthusiast
