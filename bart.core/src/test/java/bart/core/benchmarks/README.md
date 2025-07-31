# BART Performance Benchmarking Suite

This benchmarking suite measures the performance characteristics of the BART policy evaluation engine across three key metrics:

1. **Number of Policies** - How execution time scales with policy count
2. **Number of Attributes** - How execution time scales with attribute complexity  
3. **Number of Exchanges** - How execution time scales with exchange complexity

## Features

- **Three Scenario Types** for each metric:
  - **Optimal**: Success after evaluating minimal elements (best case)
  - **Average**: Success after evaluating ~50% of elements (typical case)
  - **Worst**: Denial after evaluating all elements (worst case)

- **Statistical Significance**: Multiple repetitions for reliable measurements
- **Publication-Ready Output**: CSV data and visualization scripts
- **Multiple Interfaces**: JUnit tests, standalone runners, and CLI tools

## Quick Start

### 1. Run Demo Benchmark

```bash
# From the bart.core directory
java -cp target/test-classes:target/classes bart.core.benchmarks.BenchmarkMain demo
```

### 2. Run Quick Tests (Fast)

```bash
java -cp target/test-classes:target/classes bart.core.benchmarks.BenchmarkRunner quick
```

### 3. Run Full Benchmarks (Comprehensive)

```bash
java -cp target/test-classes:target/classes bart.core.benchmarks.BenchmarkMain full
```

### 4. Run from IDE

Execute the `BenchmarkTest` JUnit class in your IDE for integrated testing.

## Benchmark Configuration

### Ranges and Steps

- **Policies**: 1-1000 (step 100)
- **Attributes**: 1-100 (step 10)  
- **Exchanges**: 1-100 (step 10)

### Baseline Values

When testing one metric independently, others are held constant:

- **Baseline Policies**: 10
- **Baseline Attributes**: 5
- **Baseline Exchanges**: 3

### Repetitions

Each measurement point is repeated 3 times and averaged for statistical reliability.

## Output Files

### CSV Data Files
- `policies_benchmark.csv` - Policy count vs performance
- `attributes_benchmark.csv` - Attribute count vs performance  
- `exchanges_benchmark.csv` - Exchange count vs performance

### Analysis Scripts
- `analyze_benchmarks.py` - Python visualization with matplotlib
- `analyze_benchmarks.R` - R analysis with ggplot2

## Usage Examples

### Individual Metric Benchmarks
```bash
# Test only policy count scaling
java bart.core.benchmarks.BenchmarkRunner policies

# Test only attribute count scaling  
java bart.core.benchmarks.BenchmarkRunner attributes

# Test only exchange count scaling
java bart.core.benchmarks.BenchmarkRunner exchanges
```

### Generate Analysis Tools Only
```bash
java bart.core.benchmarks.BenchmarkMain analyze
```

### Run Visualization
After generating CSV files:
```bash
# Using Python
python analyze_benchmarks.py

# Using R
Rscript analyze_benchmarks.R
```

## Understanding Results

### Console Output
Results are displayed in tables showing:
- **Metric Value**: The varied parameter (policies, attributes, exchanges)
- **Optimal Time**: Best-case execution time in microseconds
- **Average Time**: Typical-case execution time in microseconds
- **Worst Time**: Worst-case execution time in microseconds
- **Ratio**: Performance degradation factor (Worst/Optimal)

### CSV Format
```
Metric,Optimal_ns,Average_ns,Worst_ns,Optimal_us,Average_us,Worst_us
10,15230,28450,67890,15.23,28.45,67.89
20,18750,35200,89200,18.75,35.20,89.20
```

### Visualization Scripts
The generated Python/R scripts create:
1. **Performance plots** showing optimal/average/worst case trends
2. **Ratio plots** showing performance degradation patterns
3. **Comparison plots** normalizing all metrics for relative analysis

## Research Paper Integration

### LaTeX Tables
The `BenchmarkAnalyzer` generates LaTeX table templates:
```latex
\\begin{table}[htbp]
\\centering
\\caption{BART Performance vs Number of Policies}
\\label{tab:policy-performance}
\\begin{tabular}{|r|r|r|r|r|}
\\hline
\\textbf{Metric} & \\textbf{Optimal (μs)} & \\textbf{Average (μs)} & \\textbf{Worst (μs)} & \\textbf{Ratio} \\\\
\\hline
% Data rows here
\\hline
\\end{tabular}
\\end{table}
```

### High-Resolution Plots
Generated PNG files are 300 DPI publication quality with:
- Clear axis labels and legends
- Professional color schemes
- Trend lines and statistical annotations

## Interpreting Performance Characteristics

### Linear vs Exponential Growth
- **Linear growth**: Indicates well-scaled algorithms
- **Exponential growth**: Suggests algorithmic bottlenecks
- **Constant time**: Ideal but rare in practice

### Scenario Analysis
- **Optimal vs Worst ratio**: Shows performance variability
- **Average case**: Most representative of real-world usage
- **Worst case**: Important for performance guarantees

### Metric Comparison
Compare normalized plots to identify:
- Which metric impacts performance most
- Whether certain metrics have compound effects
- Optimal configuration strategies

## Extending the Benchmarks

### Adding New Metrics
1. Create test scenario generator in `BenchmarkRunner`
2. Add measurement method following existing patterns
3. Update CSV output and analysis scripts

### Modifying Ranges
Update constants in benchmark classes:
```java
private static final int MAX_POLICIES = 1000;
private static final int POLICY_STEP = 100;
```

### Custom Scenarios
Create specialized benchmark methods for:
- Complex policy combinations
- Real-world scenario simulation
- Stress testing edge cases

## Troubleshooting

### Common Issues
- **OutOfMemoryError**: Reduce benchmark ranges or increase heap size
- **Inconsistent results**: Ensure no other processes are using CPU during benchmarks
- **File not found**: Check working directory for CSV output files

### Performance Tips
- Run benchmarks on dedicated machine for consistency
- Use server JVM for better optimization: `-server`
- Warm up JVM before measurements: run smaller benchmarks first

## Contributing

When adding new benchmarks:
1. Follow the three-scenario pattern (optimal/average/worst)
2. Use statistical repetitions for reliability
3. Update documentation and analysis scripts
4. Test with both small and large-scale scenarios
