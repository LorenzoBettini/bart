import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns

# Set style for publication-quality plots
plt.style.use('seaborn-v0_8')
sns.set_palette("husl")

def load_and_plot_benchmark(csv_file, metric_name, title):
    try:
        # Load data
        df = pd.read_csv(csv_file)

        # Create figure with subplots
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))

        # Plot 1: Execution time vs metric
        ax1.plot(df.iloc[:, 0], df['Optimal_us'], 'o-', label='Optimal Case', linewidth=2, markersize=6)
        ax1.plot(df.iloc[:, 0], df['Average_us'], 's-', label='Average Case', linewidth=2, markersize=6)
        ax1.plot(df.iloc[:, 0], df['Worst_us'], '^-', label='Worst Case', linewidth=2, markersize=6)

        ax1.set_xlabel(metric_name, fontsize=12)
        ax1.set_ylabel('Execution Time (μs)', fontsize=12)
        ax1.set_title(f'{title} - Execution Time', fontsize=14, fontweight='bold')
        ax1.legend(fontsize=10)
        ax1.grid(True, alpha=0.3)

        # Plot 2: Performance ratio (worst/optimal)
        ratio = df['Worst_us'] / df['Optimal_us']
        ax2.plot(df.iloc[:, 0], ratio, 'o-', color='red', linewidth=2, markersize=6)
        ax2.set_xlabel(metric_name, fontsize=12)
        ax2.set_ylabel('Performance Ratio (Worst/Optimal)', fontsize=12)
        ax2.set_title(f'{title} - Performance Degradation', fontsize=14, fontweight='bold')
        ax2.grid(True, alpha=0.3)

        # Add trend line for ratio
        z = np.polyfit(df.iloc[:, 0], ratio, 1)
        p = np.poly1d(z)
        ax2.plot(df.iloc[:, 0], p(df.iloc[:, 0]), "--", alpha=0.7, label=f'Trend (slope: {z[0]:.3f})')
        ax2.legend(fontsize=10)

        plt.tight_layout()
        plt.savefig(f'{csv_file.replace(".csv", "_plot.png")}', dpi=300, bbox_inches='tight')
        plt.show()

        # Print summary statistics
        print(f"\n=== {title} Analysis ===")
        print(f"Range: {df.iloc[0, 0]} to {df.iloc[-1, 0]} {metric_name.lower()}")
        print(f"Optimal time range: {df['Optimal_us'].min():.2f} - {df['Optimal_us'].max():.2f} μs")
        print(f"Worst time range: {df['Worst_us'].min():.2f} - {df['Worst_us'].max():.2f} μs")
        print(f"Max performance ratio: {ratio.max():.2f}x")
        print(f"Average performance ratio: {ratio.mean():.2f}x")

        return df

    except FileNotFoundError:
        print(f"File {csv_file} not found. Run benchmarks first.")
        return None

def create_comparison_plot():
    # Load all benchmark data
    policies_df = pd.read_csv('policies_benchmark.csv')
    attributes_df = pd.read_csv('attributes_benchmark.csv')
    exchanges_df = pd.read_csv('exchanges_benchmark.csv')

    # Normalize data for comparison (scale to 0-1)
    def normalize_series(series):
        return (series - series.min()) / (series.max() - series.min())

    fig, ax = plt.subplots(figsize=(12, 8))

    # Plot normalized worst-case performance
    ax.plot(normalize_series(policies_df.iloc[:, 0]),
            normalize_series(policies_df['Worst_us']),
            'o-', label='Policy Count', linewidth=2, markersize=6)

    ax.plot(normalize_series(attributes_df.iloc[:, 0]),
            normalize_series(attributes_df['Worst_us']),
            's-', label='Attribute Count', linewidth=2, markersize=6)

    ax.plot(normalize_series(exchanges_df.iloc[:, 0]),
            normalize_series(exchanges_df['Worst_us']),
            '^-', label='Exchange Count', linewidth=2, markersize=6)

    ax.set_xlabel('Normalized Metric Value', fontsize=12)
    ax.set_ylabel('Normalized Execution Time', fontsize=12)
    ax.set_title('BART Performance Comparison - Worst Case Scenarios', fontsize=14, fontweight='bold')
    ax.legend(fontsize=11)
    ax.grid(True, alpha=0.3)

    plt.tight_layout()
    plt.savefig('bart_performance_comparison.png', dpi=300, bbox_inches='tight')
    plt.show()

def generate_report():
    print("\n" + "="*60)
    print("BART PERFORMANCE BENCHMARK REPORT")
    print("="*60)

    # Process each benchmark type
    policies_df = load_and_plot_benchmark('policies_benchmark.csv', 'Number of Policies', 'Policy Count Impact')
    attributes_df = load_and_plot_benchmark('attributes_benchmark.csv', 'Number of Attributes', 'Attribute Count Impact')
    exchanges_df = load_and_plot_benchmark('exchanges_benchmark.csv', 'Number of Exchanges', 'Exchange Count Impact')

    if all(df is not None for df in [policies_df, attributes_df, exchanges_df]):
        create_comparison_plot()

        print("\n" + "="*60)
        print("SUMMARY INSIGHTS")
        print("="*60)
        print("1. Check which metric has the steepest performance degradation")
        print("2. Identify if any metric shows exponential vs linear growth")
        print("3. Compare optimal vs worst-case scenarios for each metric")
        print("4. Use ratio plots to understand relative performance impact")
        print("\nAll plots saved as PNG files for inclusion in papers.")

if __name__ == "__main__":
    generate_report()

