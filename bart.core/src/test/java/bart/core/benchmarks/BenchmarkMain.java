package bart.core.benchmarks;

/**
 * Main entry point for BART benchmarking.
 * Demonstrates benchmark capabilities and generates analysis tools.
 * 
 * @author Lorenzo Bettini
 */
public class BenchmarkMain {
	
	public static void main(String[] args) {
		System.out.println("BART Performance Benchmarking Suite");
		System.out.println("===================================");
		System.out.println();
		
		if (args.length > 0) {
			switch (args[0].toLowerCase()) {
				case "quick" -> runQuickDemo();
				case "full" -> runFullBenchmarks();
				case "analyze" -> generateAnalysisTools();
				case "demo" -> runDemo();
				default -> printUsage();
			}
		} else {
			runDemo();
		}
	}
	
	private static void runDemo() {
		System.out.println("Running demonstration benchmark...");
		System.out.println();
		
		// Run a quick benchmark to demonstrate capabilities
		BenchmarkRunner runner = new BenchmarkRunner();
		runner.runQuickTest();
		
		System.out.println();
		System.out.println("Demo complete! For more options:");
		printUsage();
		
		// Generate analysis tools
		System.out.println();
		System.out.println("Generating analysis tools...");
		generateAnalysisTools();
	}
	
	private static void runQuickDemo() {
		System.out.println("Running quick benchmark demonstration...");
		BenchmarkRunner runner = new BenchmarkRunner();
		runner.runQuickTest();
	}
	
	private static void runFullBenchmarks() {
		System.out.println("Running complete benchmark suite...");
		System.out.println("This may take several minutes...");
		
		BenchmarkSuite suite = new BenchmarkSuite();
		suite.runAllBenchmarks();
		
		System.out.println();
		System.out.println("Full benchmarks complete!");
		System.out.println("CSV files generated. Use analysis tools to create plots.");
	}
	
	private static void generateAnalysisTools() {
		BenchmarkAnalyzer.generateAllAnalysisTools();
	}
	
	private static void printUsage() {
		System.out.println();
		System.out.println("Usage: java bart.core.benchmarks.BenchmarkMain [option]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  demo     - Run demonstration with analysis tools (default)");
		System.out.println("  quick    - Run quick benchmark test");
		System.out.println("  full     - Run complete benchmark suite (slow)");
		System.out.println("  analyze  - Generate analysis tools only");
		System.out.println();
		System.out.println("For JUnit integration, run BenchmarkTest class in your IDE.");
		System.out.println("For targeted benchmarks, use BenchmarkRunner with arguments:");
		System.out.println("  java bart.core.benchmarks.BenchmarkRunner [policies|attributes|exchanges|quick]");
	}
}
