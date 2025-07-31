package bart.core.benchmarks;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility class for generating analysis reports and plotting scripts from benchmark results.
 * Creates publication-ready data and Python/R scripts for visualization.
 * 
 * @author Lorenzo Bettini
 */
public class BenchmarkAnalyzer {
	
	/**
	 * Generate a Python script to create plots from benchmark CSV files
	 */
	public static void generatePythonPlotScript() {
		String script = """
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
        print(f"\\n=== {title} Analysis ===")
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
    print("\\n" + "="*60)
    print("BART PERFORMANCE BENCHMARK REPORT")
    print("="*60)
    
    # Process each benchmark type
    policies_df = load_and_plot_benchmark('policies_benchmark.csv', 'Number of Policies', 'Policy Count Impact')
    attributes_df = load_and_plot_benchmark('attributes_benchmark.csv', 'Number of Attributes', 'Attribute Count Impact') 
    exchanges_df = load_and_plot_benchmark('exchanges_benchmark.csv', 'Number of Exchanges', 'Exchange Count Impact')
    
    if all(df is not None for df in [policies_df, attributes_df, exchanges_df]):
        create_comparison_plot()
        
        print("\\n" + "="*60)
        print("SUMMARY INSIGHTS")
        print("="*60)
        print("1. Check which metric has the steepest performance degradation")
        print("2. Identify if any metric shows exponential vs linear growth")
        print("3. Compare optimal vs worst-case scenarios for each metric")
        print("4. Use ratio plots to understand relative performance impact")
        print("\\nAll plots saved as PNG files for inclusion in papers.")

if __name__ == "__main__":
    generate_report()
""";
		
		try (PrintWriter writer = new PrintWriter(new FileWriter("analyze_benchmarks.py"))) {
			writer.println(script);
			System.out.println("Python analysis script saved to: analyze_benchmarks.py");
			System.out.println("Run with: python analyze_benchmarks.py");
		} catch (IOException e) {
			System.err.println("Error saving Python script: " + e.getMessage());
		}
	}
	
	/**
	 * Generate an R script for statistical analysis and plotting
	 */
	public static void generateRPlotScript() {
		String script = """
# BART Benchmark Analysis in R
# Load required libraries
library(ggplot2)
library(dplyr)
library(readr)
library(gridExtra)
library(scales)

# Function to load and analyze benchmark data
analyze_benchmark <- function(csv_file, metric_name, title) {
  if (!file.exists(csv_file)) {
    cat("File", csv_file, "not found. Run benchmarks first.\\n")
    return(NULL)
  }
  
  # Load data
  df <- read_csv(csv_file, show_col_types = FALSE)
  metric_col <- names(df)[1]
  
  # Create performance plots
  p1 <- ggplot(df, aes(x = .data[[metric_col]])) +
    geom_line(aes(y = Optimal_us, color = "Optimal"), size = 1.2) +
    geom_point(aes(y = Optimal_us, color = "Optimal"), size = 3) +
    geom_line(aes(y = Average_us, color = "Average"), size = 1.2) +
    geom_point(aes(y = Average_us, color = "Average"), size = 3) +
    geom_line(aes(y = Worst_us, color = "Worst"), size = 1.2) +
    geom_point(aes(y = Worst_us, color = "Worst"), size = 3) +
    labs(
      title = paste(title, "- Execution Time"),
      x = metric_name,
      y = "Execution Time (μs)",
      color = "Scenario"
    ) +
    theme_minimal() +
    theme(
      plot.title = element_text(size = 14, face = "bold"),
      axis.title = element_text(size = 12),
      legend.title = element_text(size = 11),
      legend.text = element_text(size = 10)
    ) +
    scale_color_manual(values = c("Optimal" = "#2E8B57", "Average" = "#4682B4", "Worst" = "#DC143C"))
  
  # Performance ratio plot
  df$ratio <- df$Worst_us / df$Optimal_us
  
  p2 <- ggplot(df, aes(x = .data[[metric_col]], y = ratio)) +
    geom_line(color = "#DC143C", size = 1.2) +
    geom_point(color = "#DC143C", size = 3) +
    geom_smooth(method = "lm", se = TRUE, alpha = 0.2, color = "#8B0000") +
    labs(
      title = paste(title, "- Performance Degradation"),
      x = metric_name,
      y = "Performance Ratio (Worst/Optimal)"
    ) +
    theme_minimal() +
    theme(
      plot.title = element_text(size = 14, face = "bold"),
      axis.title = element_text(size = 12)
    )
  
  # Combine plots
  combined_plot <- grid.arrange(p1, p2, ncol = 2)
  
  # Save plot
  plot_filename <- gsub(".csv", "_analysis.png", csv_file)
  ggsave(plot_filename, combined_plot, width = 15, height = 6, dpi = 300)
  
  # Print statistics
  cat("\\n=== ", title, " Analysis ===\\n")
  cat("Range:", min(df[[metric_col]]), "to", max(df[[metric_col]]), tolower(metric_name), "\\n")
  cat("Optimal time range:", round(min(df$Optimal_us), 2), "-", round(max(df$Optimal_us), 2), "μs\\n")
  cat("Worst time range:", round(min(df$Worst_us), 2), "-", round(max(df$Worst_us), 2), "μs\\n")
  cat("Max performance ratio:", round(max(df$ratio), 2), "x\\n")
  cat("Average performance ratio:", round(mean(df$ratio), 2), "x\\n")
  
  return(df)
}

# Analyze each benchmark type
policies_df <- analyze_benchmark("policies_benchmark.csv", "Number of Policies", "Policy Count Impact")
attributes_df <- analyze_benchmark("attributes_benchmark.csv", "Number of Attributes", "Attribute Count Impact")
exchanges_df <- analyze_benchmark("exchanges_benchmark.csv", "Number of Exchanges", "Exchange Count Impact")

# Create comparison plot
if (!is.null(policies_df) && !is.null(attributes_df) && !is.null(exchanges_df)) {
  # Normalize data for comparison
  normalize <- function(x) (x - min(x)) / (max(x) - min(x))
  
  comparison_df <- data.frame(
    metric_normalized = c(normalize(policies_df[[1]]), normalize(attributes_df[[1]]), normalize(exchanges_df[[1]])),
    time_normalized = c(normalize(policies_df$Worst_us), normalize(attributes_df$Worst_us), normalize(exchanges_df$Worst_us)),
    benchmark_type = factor(rep(c("Policy Count", "Attribute Count", "Exchange Count"), 
                               c(nrow(policies_df), nrow(attributes_df), nrow(exchanges_df))))
  )
  
  comparison_plot <- ggplot(comparison_df, aes(x = metric_normalized, y = time_normalized, color = benchmark_type)) +
    geom_line(size = 1.5) +
    geom_point(size = 3) +
    labs(
      title = "BART Performance Comparison - Worst Case Scenarios",
      x = "Normalized Metric Value",
      y = "Normalized Execution Time",
      color = "Benchmark Type"
    ) +
    theme_minimal() +
    theme(
      plot.title = element_text(size = 16, face = "bold"),
      axis.title = element_text(size = 13),
      legend.title = element_text(size = 12),
      legend.text = element_text(size = 11)
    ) +
    scale_color_manual(values = c("#2E8B57", "#4682B4", "#DC143C"))
  
  ggsave("bart_performance_comparison.png", comparison_plot, width = 12, height = 8, dpi = 300)
  print(comparison_plot)
  
  cat("\\n", paste(rep("=", 60), collapse = ""), "\\n")
  cat("SUMMARY INSIGHTS\\n")
  cat(paste(rep("=", 60), collapse = ""), "\\n")
  cat("1. Check which metric shows the steepest performance growth\\n")
  cat("2. Analyze linearity vs exponential growth patterns\\n") 
  cat("3. Compare optimal vs worst-case performance gaps\\n")
  cat("4. Identify the most performance-critical metric\\n")
  cat("\\nAll plots saved as high-resolution PNG files.\\n")
}
""";
		
		try (PrintWriter writer = new PrintWriter(new FileWriter("analyze_benchmarks.R"))) {
			writer.println(script);
			System.out.println("R analysis script saved to: analyze_benchmarks.R");
			System.out.println("Run with: Rscript analyze_benchmarks.R");
		} catch (IOException e) {
			System.err.println("Error saving R script: " + e.getMessage());
		}
	}
	
	/**
	 * Generate a comprehensive LaTeX table for research paper inclusion
	 */
	public static void generateLatexTable(String csvFile, String caption, String label) {
		try {
			System.out.println("\\n% LaTeX table for " + caption);
			System.out.println("\\begin{table}[htbp]");
			System.out.println("\\centering");
			System.out.println("\\caption{" + caption + "}");
			System.out.println("\\label{" + label + "}");
			System.out.println("\\begin{tabular}{|r|r|r|r|r|}");
			System.out.println("\\hline");
			System.out.println("\\textbf{Metric} & \\textbf{Optimal ($\\mu$s)} & \\textbf{Average ($\\mu$s)} & \\textbf{Worst ($\\mu$s)} & \\textbf{Ratio} \\\\");
			System.out.println("\\hline");
			
			// Note: In a real implementation, you'd read the CSV file here
			System.out.println("% Add data rows from " + csvFile);
			System.out.println("% Example: 10 & 15.23 & 28.45 & 67.89 & 4.46 \\\\");
			
			System.out.println("\\hline");
			System.out.println("\\end{tabular}");
			System.out.println("\\end{table}");
			
		} catch (Exception e) {
			System.err.println("Error generating LaTeX table: " + e.getMessage());
		}
	}
	
	/**
	 * Generate analysis scripts and documentation
	 */
	public static void generateAllAnalysisTools() {
		System.out.println("Generating analysis tools for BART benchmarks...");
		
		generatePythonPlotScript();
		generateRPlotScript();
		
		System.out.println("\nGenerated analysis tools:");
		System.out.println("- analyze_benchmarks.py (Python with matplotlib)");
		System.out.println("- analyze_benchmarks.R (R with ggplot2)");
		System.out.println("\nTo use:");
		System.out.println("1. Run benchmarks to generate CSV files");
		System.out.println("2. Run: python analyze_benchmarks.py OR Rscript analyze_benchmarks.R");
		System.out.println("3. Check generated PNG files for publication-ready plots");
		
		// Generate example LaTeX tables
		generateLatexTable("policies_benchmark.csv", 
			"BART Performance vs Number of Policies", "tab:policy-performance");
		generateLatexTable("attributes_benchmark.csv", 
			"BART Performance vs Number of Attributes", "tab:attribute-performance");
		generateLatexTable("exchanges_benchmark.csv", 
			"BART Performance vs Number of Exchanges", "tab:exchange-performance");
	}
}
