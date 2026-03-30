# BART Performance Tests

This package contains performance tests for the BART project to measure how execution time increases with various metrics.

## Overview

The performance tests measure the impact of five key metrics on evaluation time:

1. **Number of Policies** - How execution time scales with the number of policies in the system
2. **Number of Attributes** - How attribute count affects matching and evaluation performance
3. **Number of Exchanges** - How the width of AND exchange chains impacts performance
4. **Exchange Chain Depth** - How the depth of recursive exchange chains impacts performance
5. **Exponential Tree Depth** - How a binary AND-exchange tree grows exponentially with depth

Each metric is tested independently while keeping other factors constant.

## Configuration

### Baseline Configuration (Constants)

When measuring one metric, the others are held at these baseline values:

- **Policies**: 10
- **Attributes per entity**: 5
- **Exchanges per rule**: 3

### Measurement Ranges

The tests use these ranges and steps:

- **Number of Policies**: 100, then 1000 to 10000 (step 1000)
- **Number of Attributes**: 10, then 100 to 1000 (step 100)
- **Number of Exchanges**: 1, then 10 to 160 (step 15)
- **Exchange Chain Depth**: 2, then 10 to 160 (step 15)
- **Exponential Tree Depth**: 2, then 3 to 10 (step 1)

### Test Parameters

- **Default repetitions**: 100 iterations per measurement point
- **Exponential tree repetitions**: 3 iterations (temporarily reduced in that scenario to avoid very long runs)
- **Warm-up**: 20 iterations before measurements begin

All these values can be adjusted by modifying constants and parameters in the `PerformanceStatistics` class.

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

The `PerformanceStatistics` class has a `main()` method, so you can run it directly from your IDE like any other Java application.

## Output Format

The tests output results in a text-based table format showing:

- **Metric Value**: The value being tested (e.g., number of policies or depth)
- **Avg Time (ms)**: Average execution time across repetitions
- **Min Time (ms)**: Minimum execution time observed
- **Max Time (ms)**: Maximum execution time observed
- **Std Dev (ms)**: Standard deviation of execution times

Example output:

```
================================================================================
BART Performance Tests
================================================================================

Warming up JVM with 20 iterations...
Warm-up complete.

Running performance tests...

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
100             1.051263             0.720266             2.334211             0.282896            
1000            2.513296             1.590188             8.095957             1.438809            
2000            3.660196             3.134469             6.483130             0.617969            
3000            5.309066             4.457085             7.918783             0.645742            
4000            7.149537             6.360736             12.061211            0.733808            
5000            8.899375             8.161262             10.512052            0.530080            
6000            10.752564            9.545741             13.097545            0.733166            
7000            13.175976            11.528444            22.088137            1.496799            
8000            14.546005            13.627136            17.157248            0.889353            
9000            18.117213            15.012500            39.342158            4.377790            
10000           18.585067            17.069912            21.633398            1.057566            
--------------------------------------------------------------------------------

...
```

## Test Methodology

### JVM Warm-up

The tests perform 20 warm-up iterations before collecting measurements to ensure the JVM is optimized (JIT compilation, class loading, etc.).

### Test Scenarios

For each metric test:

1. **Policy Count Test**:
   - Creates N policies where only the last one matches the request
   - Measures time to evaluate a request that must check all policies before finding the match
   - Tests a worst-case policy-selection path

2. **Attribute Count Test**:
   - Varies the number of attributes in party definitions
   - All policies have the same number of attributes
   - Measures the overhead of attribute matching

3. **Exchange Count Test**:
   - Creates AND chains of exchanges of varying length
   - All exchanges can be satisfied by the requester's policy
   - Measures the overhead of exchange evaluation (width)

4. **Exchange Chain Depth Test**:
   - Creates a chain of N policies where each policy's exchange references the next policy
   - The last policy closes the chain back to the requester
   - Measures the overhead of recursive exchange evaluation (depth)

5. **Exponential Tree Depth Test**:
   - Implements a binary dependency tree
   - Each internal node requires two subordinate requests through an AND exchange
   - Leaf nodes grant access unconditionally
   - The number of evaluation nodes follows $2^{d+1}-1$
   - Repetitions are reduced to 3 in this scenario to keep execution time reasonable

### Verification

Each measurement includes an assertion to verify that the request was permitted, ensuring the scenario is valid.

## Modifying the Tests

To adjust test parameters, edit `PerformanceStatistics`:

```java
// Baseline configuration
private static final int BASELINE_NUM_POLICIES = 10;
private static final int BASELINE_NUM_ATTRIBUTES = 5;
private static final int BASELINE_NUM_EXCHANGES = 3;

// Measurement ranges
private static final int POLICIES_MIN = 1000;
private static final int POLICIES_MAX = 10000;
private static final int POLICIES_STEP = 1000;

private static final int EXCHANGE_DEPTH_MIN = 10;
private static final int EXCHANGE_DEPTH_MAX = 160;
private static final int EXCHANGE_DEPTH_STEP = 15;

private static final int TREE_DEPTH_MIN = 3;
private static final int TREE_DEPTH_MAX = 10;
private static final int TREE_DEPTH_STEP = 1;

// Repetitions (used by most scenarios)
private static int repetitions = 100;
```
