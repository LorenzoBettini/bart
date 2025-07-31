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
        
        # Create figure
        fig, ax = plt.subplots(1, 1, figsize=(10, 6))
        
        # Plot execution time vs metric
        ax.plot(df.iloc[:, 0], df['Time_us'], 'o-', color='blue', linewidth=2, markersize=6)
        
        ax.set_xlabel(metric_name, fontsize=12)
        ax.set_ylabel('Execution Time (μs)', fontsize=12)
        ax.set_title(f'{title} - Execution Time vs {metric_name}', fontsize=14, fontweight='bold')
        ax.grid(True, alpha=0.3)
        
        # Add trend line
        z = np.polyfit(df.iloc[:, 0], df['Time_us'], 1)
        p = np.poly1d(z)
        ax.plot(df.iloc[:, 0], p(df.iloc[:, 0]), "--", alpha=0.8, color='red', label=f'Trend (slope: {z[0]:.2f})')
        ax.legend()
        
        plt.tight_layout()
        
        # Print statistics
        print(f"\\n{title} Statistics:")
        print(f"Time range: {df['Time_us'].min():.2f} - {df['Time_us'].max():.2f} μs")
        print(f"Average time: {df['Time_us'].mean():.2f} μs")
        print(f"Scaling factor: {df['Time_us'].max() / df['Time_us'].min():.2f}x")
        
        # Calculate correlation coefficient
        correlation = np.corrcoef(df.iloc[:, 0], df['Time_us'])[0, 1]
        print(f"Correlation coefficient: {correlation:.3f}")
        
        return fig
    except Exception as e:
        print(f"Error plotting {csv_file}: {e}")
        return None

def create_combined_plot():
    try:
        # Load all benchmark data
        policies_df = pd.read_csv('policies_benchmark.csv')
        attributes_df = pd.read_csv('attributes_benchmark.csv')
        exchanges_df = pd.read_csv('exchanges_benchmark.csv')
        
        # Normalize data for comparison
        def normalize_series(series):
            return (series - series.min()) / (series.max() - series.min())
        
        # Create combined comparison plot
        fig, axes = plt.subplots(2, 2, figsize=(15, 12))
        
        # Individual plots
        axes[0, 0].plot(policies_df['Policies'], policies_df['Time_us'], 'o-', color='blue', linewidth=2)
        axes[0, 0].set_title('Policies vs Execution Time')
        axes[0, 0].set_xlabel('Number of Policies')
        axes[0, 0].set_ylabel('Time (μs)')
        axes[0, 0].grid(True, alpha=0.3)
        
        axes[0, 1].plot(attributes_df['Attributes'], attributes_df['Time_us'], 'o-', color='green', linewidth=2)
        axes[0, 1].set_title('Attributes vs Execution Time')
        axes[0, 1].set_xlabel('Number of Attributes')
        axes[0, 1].set_ylabel('Time (μs)')
        axes[0, 1].grid(True, alpha=0.3)
        
        axes[1, 0].plot(exchanges_df['Exchanges'], exchanges_df['Time_us'], 'o-', color='red', linewidth=2)
        axes[1, 0].set_title('Exchanges vs Execution Time')
        axes[1, 0].set_xlabel('Number of Exchanges')
        axes[1, 0].set_ylabel('Time (μs)')
        axes[1, 0].grid(True, alpha=0.3)
        
        # Normalized comparison plot
        max_len = min(len(policies_df), len(attributes_df), len(exchanges_df))
        x_normalized = np.linspace(0, 1, max_len)
        
        axes[1, 1].plot(x_normalized, normalize_series(policies_df['Time_us'][:max_len]), 'o-', label='Policies', linewidth=2)
        axes[1, 1].plot(x_normalized, normalize_series(attributes_df['Time_us'][:max_len]), 's-', label='Attributes', linewidth=2)
        axes[1, 1].plot(x_normalized, normalize_series(exchanges_df['Time_us'][:max_len]), '^-', label='Exchanges', linewidth=2)
        axes[1, 1].set_title('Normalized Comparison')
        axes[1, 1].set_xlabel('Normalized Metric Range (0-1)')
        axes[1, 1].set_ylabel('Normalized Execution Time (0-1)')
        axes[1, 1].legend()
        axes[1, 1].grid(True, alpha=0.3)
        
        plt.tight_layout()
        return fig
    except Exception as e:
        print(f"Error creating combined plot: {e}")
        return None

# Generate all plots
print("Generating BART Performance Analysis Plots...")

# Individual plots
fig1 = load_and_plot_benchmark('policies_benchmark.csv', 'Number of Policies', 'Policy Scaling')
if fig1:
    fig1.savefig('policies_performance.png', dpi=300, bbox_inches='tight')

fig2 = load_and_plot_benchmark('attributes_benchmark.csv', 'Number of Attributes', 'Attribute Scaling')
if fig2:
    fig2.savefig('attributes_performance.png', dpi=300, bbox_inches='tight')

fig3 = load_and_plot_benchmark('exchanges_benchmark.csv', 'Number of Exchanges', 'Exchange Scaling')
if fig3:
    fig3.savefig('exchanges_performance.png', dpi=300, bbox_inches='tight')

# Combined plot
fig4 = create_combined_plot()
if fig4:
    fig4.savefig('combined_performance.png', dpi=300, bbox_inches='tight')

plt.show()
print("Plots saved as PNG files.")
""";
		
		try (PrintWriter writer = new PrintWriter(new FileWriter("plot_benchmarks.py"))) {
			writer.print(script);
			System.out.println("Generated: plot_benchmarks.py");
		} catch (IOException e) {
			System.err.println("Error generating Python script: " + e.getMessage());
		}
	}
	
	/**
	 * Generate an R script to create plots from benchmark CSV files
	 */
	public static void generateRPlotScript() {
		String script = """
# Load required libraries
library(ggplot2)
library(dplyr)
library(gridExtra)

# Set theme for publication-quality plots
theme_set(theme_minimal() + theme(
  plot.title = element_text(size = 14, face = "bold"),
  axis.title = element_text(size = 12),
  axis.text = element_text(size = 10),
  legend.text = element_text(size = 10)
))

# Function to load and plot benchmark data
load_and_plot_benchmark <- function(csv_file, metric_name, title) {
  tryCatch({
    # Load data
    df <- read.csv(csv_file)
    
    # Create plot
    p <- ggplot(df, aes_string(x = names(df)[1], y = "Time_us")) +
      geom_line(color = "blue", size = 1.2) +
      geom_point(color = "blue", size = 3) +
      geom_smooth(method = "lm", se = TRUE, alpha = 0.3, color = "red", linetype = "dashed") +
      labs(
        title = paste(title, "- Execution Time vs", metric_name),
        x = metric_name,
        y = "Execution Time (μs)"
      ) +
      theme(panel.grid = element_line(alpha = 0.3))
    
    # Print statistics
    cat("\\n", title, "Statistics:\\n")
    cat("Time range:", round(min(df$Time_us), 2), "-", round(max(df$Time_us), 2), "μs\\n")
    cat("Average time:", round(mean(df$Time_us), 2), "μs\\n")
    cat("Scaling factor:", round(max(df$Time_us) / min(df$Time_us), 2), "x\\n")
    
    # Calculate correlation coefficient
    correlation <- cor(df[,1], df$Time_us)
    cat("Correlation coefficient:", round(correlation, 3), "\\n")
    
    return(p)
  }, error = function(e) {
    cat("Error plotting", csv_file, ":", e$message, "\\n")
    return(NULL)
  })
}

# Function to create combined comparison plot
create_combined_plot <- function() {
  tryCatch({
    # Load all benchmark data
    policies_df <- read.csv("policies_benchmark.csv")
    attributes_df <- read.csv("attributes_benchmark.csv")
    exchanges_df <- read.csv("exchanges_benchmark.csv")
    
    # Normalize function
    normalize_series <- function(x) {
      (x - min(x)) / (max(x) - min(x))
    }
    
    # Create normalized comparison data
    max_len <- min(nrow(policies_df), nrow(attributes_df), nrow(exchanges_df))
    
    combined_df <- data.frame(
      x_norm = rep(seq(0, 1, length.out = max_len), 3),
      time_norm = c(
        normalize_series(policies_df$Time_us[1:max_len]),
        normalize_series(attributes_df$Time_us[1:max_len]),
        normalize_series(exchanges_df$Time_us[1:max_len])
      ),
      metric = rep(c("Policies", "Attributes", "Exchanges"), each = max_len)
    )
    
    # Create comparison plot
    p <- ggplot(combined_df, aes(x = x_norm, y = time_norm, color = metric)) +
      geom_line(size = 1.2) +
      geom_point(size = 3) +
      labs(
        title = "BART Performance Scaling Comparison",
        x = "Normalized Metric Range (0-1)",
        y = "Normalized Execution Time (0-1)",
        color = "Metric Type"
      ) +
      scale_color_manual(values = c("Policies" = "blue", "Attributes" = "green", "Exchanges" = "red")) +
      theme(panel.grid = element_line(alpha = 0.3))
    
    return(p)
  }, error = function(e) {
    cat("Error creating combined plot:", e$message, "\\n")
    return(NULL)
  })
}

# Generate all plots
cat("Generating BART Performance Analysis Plots...\\n")

# Individual plots
p1 <- load_and_plot_benchmark("policies_benchmark.csv", "Number of Policies", "Policy Scaling")
p2 <- load_and_plot_benchmark("attributes_benchmark.csv", "Number of Attributes", "Attribute Scaling")
p3 <- load_and_plot_benchmark("exchanges_benchmark.csv", "Number of Exchanges", "Exchange Scaling")

# Combined plot
p4 <- create_combined_plot()

# Save plots
if (!is.null(p1)) {
  ggsave("policies_performance.png", p1, width = 10, height = 6, dpi = 300)
}
if (!is.null(p2)) {
  ggsave("attributes_performance.png", p2, width = 10, height = 6, dpi = 300)
}
if (!is.null(p3)) {
  ggsave("exchanges_performance.png", p3, width = 10, height = 6, dpi = 300)
}
if (!is.null(p4)) {
  ggsave("combined_performance.png", p4, width = 12, height = 8, dpi = 300)
}

# Display plots
if (!is.null(p1) && !is.null(p2) && !is.null(p3)) {
  grid.arrange(p1, p2, p3, ncol = 2)
}

if (!is.null(p4)) {
  print(p4)
}

cat("Plots saved as PNG files.\\n")
""";
		
		try (PrintWriter writer = new PrintWriter(new FileWriter("plot_benchmarks.R"))) {
			writer.print(script);
			System.out.println("Generated: plot_benchmarks.R");
		} catch (IOException e) {
			System.err.println("Error generating R script: " + e.getMessage());
		}
	}
	
	/**
	 * Generate a LaTeX table template for including benchmark results in papers
	 */
	public static void generateLatexTable() {
		String template = """
% LaTeX table template for BART benchmark results
% Include this in your paper and replace with actual data

\\begin{table}[htbp]
\\centering
\\caption{BART Performance Benchmark Results}
\\label{tab:bart-performance}
\\begin{tabular}{|l|r|r|r|}
\\hline
\\textbf{Metric} & \\textbf{Range} & \\textbf{Time Range (μs)} & \\textbf{Scaling} \\\\
\\hline
Policies & 1-1000 & X.XX - X,XXX.XX & Linear (O(n)) \\\\
Attributes & 1-100 & X.XX - XXX.XX & Constant (O(1)) \\\\
Exchanges & 1-100 & X.XX - XXX.XX & Moderate (O(k)) \\\\
\\hline
\\end{tabular}
\\end{table}

% Usage instructions:
% 1. Run the benchmark: mvn exec:java -Dexec.mainClass="bart.core.benchmarks.ExtendedBenchmark"
% 2. Replace X.XX values with actual results from CSV files
% 3. Include in your LaTeX document's table section
% 4. Reference with \\ref{tab:bart-performance}
""";
		
		try (PrintWriter writer = new PrintWriter(new FileWriter("benchmark_table.tex"))) {
			writer.print(template);
			System.out.println("Generated: benchmark_table.tex");
		} catch (IOException e) {
			System.err.println("Error generating LaTeX template: " + e.getMessage());
		}
	}
	
	/**
	 * Generate all analysis tools
	 */
	public static void generateAllAnalysisTools() {
		generatePythonPlotScript();
		generateRPlotScript();
		generateLatexTable();
		System.out.println("\nAnalysis tools generated:");
		System.out.println("- plot_benchmarks.py (Python matplotlib)");
		System.out.println("- plot_benchmarks.R (R ggplot2)");
		System.out.println("- benchmark_table.tex (LaTeX table template)");
		System.out.println("\nTo use:");
		System.out.println("  Python: python plot_benchmarks.py");
		System.out.println("  R: Rscript plot_benchmarks.R");
	}
}