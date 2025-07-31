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
    cat("File", csv_file, "not found. Run benchmarks first.\n")
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
  cat("\n=== ", title, " Analysis ===\n")
  cat("Range:", min(df[[metric_col]]), "to", max(df[[metric_col]]), tolower(metric_name), "\n")
  cat("Optimal time range:", round(min(df$Optimal_us), 2), "-", round(max(df$Optimal_us), 2), "μs\n")
  cat("Worst time range:", round(min(df$Worst_us), 2), "-", round(max(df$Worst_us), 2), "μs\n")
  cat("Max performance ratio:", round(max(df$ratio), 2), "x\n")
  cat("Average performance ratio:", round(mean(df$ratio), 2), "x\n")

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

  cat("\n", paste(rep("=", 60), collapse = ""), "\n")
  cat("SUMMARY INSIGHTS\n")
  cat(paste(rep("=", 60), collapse = ""), "\n")
  cat("1. Check which metric shows the steepest performance growth\n")
  cat("2. Analyze linearity vs exponential growth patterns\n")
  cat("3. Compare optimal vs worst-case performance gaps\n")
  cat("4. Identify the most performance-critical metric\n")
  cat("\nAll plots saved as high-resolution PNG files.\n")
}

