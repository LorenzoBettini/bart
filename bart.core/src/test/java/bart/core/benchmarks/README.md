# BART Performance Benchmarking Suite

This benchmarking suite measures the performance characteristics of the BART policy evaluation engine across three key metrics:

1. **Number of Policies** - How execution time scales with policy count
2. **Number of Attributes** - How execution time scales with attribute complexity  
3. **Number of Exchanges** - How execution time scales with exchange complexity

## Features

- **Consistent Middle-Position Matching**: All tests use middle-position matching for reliable, consistent results
- **Statistical Significance**: Multiple repetitions with JVM warm-up for reliable measurements
- **Research-Ready Output**: Clean CSV data suitable for academic publications
- **Multiple Test Scales**: Quick tests for development, comprehensive tests for research

## Prerequisites

- Java 21+
- Maven 3.6+

## Quick Start

### Using Maven (Recommended)

```bash
# From the bart.core directory - compile first
mvn compile test-compile

# Quick benchmark test (small ranges)
mvn exec:java -Dexec.mainClass="bart.core.benchmarks.SimpleBenchmark" -Dexec.classpathScope=test

# Complete benchmarks for research (large ranges)
mvn exec:java -Dexec.mainClass="bart.core.benchmarks.ExtendedBenchmark" -Dexec.classpathScope=test

# Individual metrics
mvn exec:java -Dexec.mainClass="bart.core.benchmarks.ExtendedBenchmark" -Dexec.args="policies" -Dexec.classpathScope=test
mvn exec:java -Dexec.mainClass="bart.core.benchmarks.ExtendedBenchmark" -Dexec.args="attributes" -Dexec.classpathScope=test
mvn exec:java -Dexec.mainClass="bart.core.benchmarks.ExtendedBenchmark" -Dexec.args="exchanges" -Dexec.classpathScope=test

# Quick test with smaller ranges  
mvn exec:java -Dexec.mainClass="bart.core.benchmarks.ExtendedBenchmark" -Dexec.args="quick" -Dexec.classpathScope=test
```

### Direct Java Execution

```bash
# From the bart.core directory - ensure compilation
mvn compile test-compile

# Quick test
java -cp target/classes:target/test-classes bart.core.benchmarks.SimpleBenchmark

# Complete benchmarks
java -cp target/classes:target/test-classes bart.core.benchmarks.ExtendedBenchmark

# Individual metrics
java -cp target/classes:target/test-classes bart.core.benchmarks.ExtendedBenchmark policies
java -cp target/classes:target/test-classes bart.core.benchmarks.ExtendedBenchmark attributes
java -cp target/classes:target/test-classes bart.core.benchmarks.ExtendedBenchmark exchanges
```

## Benchmark Configuration

### Test Ranges

**SimpleBenchmark** (Quick Development Testing):

- **Policies**: 1, 5, 10, 15, 20
- **Attributes**: 1, 3, 6, 9, 12, 15
- **Exchanges**: 1, 2, 4, 6, 8

**ExtendedBenchmark** (Research-Quality Testing):

- **Policies**: 1-1000 (step 50) - 20 data points
- **Attributes**: 1-100 (step 10) - 10 data points  
- **Exchanges**: 1-100 (step 10) - 10 data points

### Repetitions

Each benchmark runs 3 repetitions per data point, plus JVM warm-up cycles to eliminate cold start bias.

## Output

The benchmarks generate CSV files in the current directory:

- `policies_benchmark.csv` - Policy count vs execution time
- `attributes_benchmark.csv` - Attribute count vs execution time  
- `exchanges_benchmark.csv` - Exchange count vs execution time

## CSV Format

```csv
Policies,Time_us
1,172
51,954
101,1836
...
```

The CSV files use consistent column naming:

- First column: The varied parameter (Policies, Attributes, or Exchanges)
- Second column: Execution time in microseconds (Time_us)

## Example Output

```text
=== Policy Count Benchmark ===
Testing with 1 policies: 172 μs
Testing with 51 policies: 954 μs  
Testing with 101 policies: 1836 μs
...
Benchmark complete! CSV files generated for analysis.
```

## Performance Characteristics

Based on typical benchmark results:

- **Policy Scaling**: Nearly linear growth (172μs → 6,946μs for 1-1000 policies)
- **Attribute Scaling**: Mostly constant with slight growth (47μs → 227μs for 1-100 attributes)  
- **Exchange Scaling**: Moderate growth (50μs → 106μs for 1-100 exchanges)

## Use in Research Papers

The CSV output is ready for import into statistical analysis tools:

```r
# R example
data <- read.csv("policies_benchmark.csv")
plot(data$Policies, data$Time_us, type="l")
```

```python
# Python example
import pandas as pd
import matplotlib.pyplot as plt
data = pd.read_csv("policies_benchmark.csv")
plt.plot(data["Policies"], data["Time_us"])
plt.show()
```

## Technical Notes

- All measurements use `System.nanoTime()` for nanosecond precision
- JVM warm-up prevents cold start timing artifacts
- Middle-position matching ensures consistent, reproducible results
- Each test scenario isolates one variable while keeping others constant
