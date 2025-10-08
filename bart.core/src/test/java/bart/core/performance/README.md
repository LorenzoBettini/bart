# BART Performance Tests

This package contains performance tests for the BART project to measure how execution time increases with various metrics.

## Overview

The performance tests measure the impact of three key metrics on evaluation time:

1. **Number of Policies** - How execution time scales with the number of policies in the system
2. **Number of Attributes** - How attribute count affects matching and evaluation performance
3. **Number of Exchanges** - How the complexity of exchange chains impacts performance

Each metric is tested independently while keeping other factors constant.

## Configuration

### Baseline Configuration (Constants)

When measuring one metric, the others are held at these baseline values:

- **Policies**: 10
- **Attributes per entity**: 5
- **Exchanges per rule**: 3

### Measurement Ranges

The tests use larger ranges and steps to clearly demonstrate performance characteristics:

- **Number of Policies**: 100, 1000, 2000, ..., 10000 (sequence: 100, then 1000 to 10000 in steps of 1000)
- **Number of Attributes**: 10, 100, 200, ..., 1000 (sequence: 10, then 100 to 1000 in steps of 100)
- **Number of Exchanges**: 1, 10, 20, ..., 100 (sequence: 1, then 10 to 100 in steps of 10)

### Test Parameters

- **Repetitions**: 100 iterations per measurement point for statistical significance
- **Warm-up**: 20 iterations before measurements begin

All these values can be adjusted by modifying the constants at the top of the `PerformanceTests` class.

## Running the Tests

### Option 1: Using Maven (Recommended)

Run the performance tests using the dedicated Maven profile:

```bash
mvn test -Pperformance-tests
```

This will:
1. Compile the code
2. Run all regular unit tests
3. Execute the performance tests
4. Display results in the console

### Option 2: Direct Execution

You can also run the performance tests directly as a Java application:

```bash
# First, compile the code
mvn test-compile

# Then run the main class
java -cp target/test-classes:target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout) \
  bart.core.performance.PerformanceStatistics
```

### Option 3: From IDE

The `PerformanceTests` class has a `main()` method, so you can run it directly from your IDE like any other Java application.

## Output Format

The tests output results in a text-based table format showing:

- **Metric Value**: The value being tested (e.g., number of policies)
- **Avg Time (ms)**: Average execution time across repetitions
- **Min Time (ms)**: Minimum execution time observed
- **Max Time (ms)**: Maximum execution time observed
- **Std Dev (ms)**: Standard deviation of execution times

Example output:

```
--------------------------------------------------------------------------------
Performance Test: Number of Policies
--------------------------------------------------------------------------------
Configuration:
  - Attributes per party: 5 (constant)
  - Exchanges per rule: 3 (constant)
  - Policies sequence: 100, 1000 to 10000 (step: 1000)
  - Repetitions: 100

Metric Value    Avg Time (ms)        Min Time (ms)        Max Time (ms)        Std Dev (ms)        
--------------------------------------------------------------------------------
100             0.358357             0.157991             2.521214             0.250818            
1000            1.404059             1.029694             3.616508             0.414532            
2000            2.258911             1.936461             4.038763             0.502164            
3000            3.302350             2.886728             7.271459             0.716855            
...
10000           10.507216            9.442787             16.578905            1.638301            
```

## Test Methodology

### JVM Warm-up

The tests perform 10 warm-up iterations before collecting measurements to ensure the JVM is optimized (JIT compilation, class loading, etc.).

### Test Scenarios

For each metric test:

1. **Policy Count Test**:
   - Creates N policies where only the last one matches the request
   - Measures time to evaluate a request that must check all policies before finding the match
   - Tests worst-case scenario where the matching policy is at the end

2. **Attribute Count Test**:
   - Varies the number of attributes in party definitions
   - All policies have the same number of attributes
   - Measures the overhead of attribute matching

3. **Exchange Count Test**:
   - Creates AND chains of exchanges of varying length
   - All exchanges can be satisfied by the requester's policy
   - Measures the overhead of exchange evaluation

### Verification

Each measurement includes an assertion to verify that the request was actually permitted, ensuring the test scenario is valid.

## Modifying the Tests

To adjust the test parameters, edit the constants in the `PerformanceTests` class:

```java
// Baseline Configuration
private static final int BASELINE_NUM_POLICIES = 10;
private static final int BASELINE_NUM_ATTRIBUTES = 5;
private static final int BASELINE_NUM_EXCHANGES = 3;

// Measurement Ranges
private static final int POLICIES_MIN = 1;
private static final int POLICIES_MAX = 1000;
private static final int POLICIES_STEP = 100;

// ... etc.
```

## Notes

- The performance tests are excluded from regular test runs to avoid slowing down normal development
- Results may vary based on hardware, JVM version, and system load
- For more accurate results, consider increasing the repetition count or running with a dedicated benchmarking tool like JMH
