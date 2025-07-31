package bart.core.benchmarks;

import static bart.core.Participants.anySuchThat;
import static bart.core.Participants.index;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import bart.core.Attributes;
import bart.core.Policies;
import bart.core.Policy;
import bart.core.Request;
import bart.core.Result;
import bart.core.Rule;
import bart.core.Rules;
import bart.core.semantics.Semantics;

/**
 * Simple benchmark runner without JUnit dependencies.
 * Can be executed standalone to test BART performance characteristics.
 * 
 * @author Lorenzo Bettini
 */
public class SimpleBenchmark {
	
	private static final int REPETITIONS = 3;
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
	
	public static void main(String[] args) {
		SimpleBenchmark benchmark = new SimpleBenchmark();
		
		System.out.println("BART Simple Performance Benchmark");
		System.out.println("=================================");
		System.out.println();
		
		// JVM warm-up to avoid cold start bias
		System.out.println("Warming up JVM...");
		benchmark.warmUpJvm();
		System.out.println("Warm-up complete. Starting benchmarks...");
		System.out.println();
		
		// Run quick benchmarks to demonstrate the concept
		benchmark.runPolicyBenchmark();
		benchmark.runAttributeBenchmark();
		
		// Generate analysis tools
		BenchmarkAnalyzer.generateAllAnalysisTools();
	}
	
	/**
	 * Warm up the JVM to avoid cold start bias in benchmark measurements.
	 * Runs several iterations with typical scenarios to trigger JIT compilation.
	 */
	private void warmUpJvm() {
		// Create a typical scenario for warm-up
		Policies warmupPolicies = createPoliciesScenario(10, 5);
		Request warmupRequest = createRequest("target");
		
		// Run multiple warm-up iterations
		for (int i = 0; i < 20; i++) {
			Semantics semantics = new Semantics(warmupPolicies);
			semantics.evaluate(warmupRequest);
		}
		
		// Force garbage collection to clean up warm-up objects
		System.gc();
		
		// Small delay to let GC complete
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	public void runPolicyBenchmark() {
		System.out.println("Policy Count Benchmark");
		System.out.println("---------------------");
		System.out.printf("%-10s %-15s %-15s %-15s%n", "Policies", "Optimal (μs)", "Average (μs)", "Worst (μs)");
		System.out.println("-".repeat(65));
		
		List<String[]> csvData = new ArrayList<>();
		csvData.add(new String[]{"Policies", "Optimal_us", "Average_us", "Worst_us"});
		
		int[] policyCounts = {1, 5, 10, 20, 50};
		
		for (int numPolicies : policyCounts) {
			BenchmarkResult result = benchmarkPolicies(numPolicies);
			
			double optimalMicros = result.optimalTime() / 1000.0;
			double averageMicros = result.averageTime() / 1000.0;
			double worstMicros = result.worstTime() / 1000.0;
			
			System.out.printf("%-10d %-15s %-15s %-15s%n",
				numPolicies,
				DECIMAL_FORMAT.format(optimalMicros),
				DECIMAL_FORMAT.format(averageMicros),
				DECIMAL_FORMAT.format(worstMicros));
			
			csvData.add(new String[]{
				String.valueOf(numPolicies),
				String.format("%.2f", optimalMicros),
				String.format("%.2f", averageMicros),
				String.format("%.2f", worstMicros)
			});
		}
		
		saveCSV("simple_policies_benchmark.csv", csvData);
		System.out.println();
	}
	
	public void runAttributeBenchmark() {
		System.out.println("Attribute Count Benchmark");
		System.out.println("-------------------------");
		System.out.printf("%-12s %-15s %-15s %-15s%n", "Attributes", "Optimal (μs)", "Average (μs)", "Worst (μs)");
		System.out.println("-".repeat(67));
		
		List<String[]> csvData = new ArrayList<>();
		csvData.add(new String[]{"Attributes", "Optimal_us", "Average_us", "Worst_us"});
		
		int[] attributeCounts = {1, 3, 5, 10, 15};
		
		for (int numAttributes : attributeCounts) {
			BenchmarkResult result = benchmarkAttributes(numAttributes);
			
			double optimalMicros = result.optimalTime() / 1000.0;
			double averageMicros = result.averageTime() / 1000.0;
			double worstMicros = result.worstTime() / 1000.0;
			
			System.out.printf("%-12d %-15s %-15s %-15s%n",
				numAttributes,
				DECIMAL_FORMAT.format(optimalMicros),
				DECIMAL_FORMAT.format(averageMicros),
				DECIMAL_FORMAT.format(worstMicros));
			
			csvData.add(new String[]{
				String.valueOf(numAttributes),
				String.format("%.2f", optimalMicros),
				String.format("%.2f", averageMicros),
				String.format("%.2f", worstMicros)
			});
		}
		
		saveCSV("simple_attributes_benchmark.csv", csvData);
		System.out.println();
	}
	
	private BenchmarkResult benchmarkPolicies(int numPolicies) {
		// Optimal: First policy matches and succeeds immediately
		Policies optimalPolicies = createPoliciesScenario(numPolicies, 1);
		Request optimalRequest = createRequest("target");
		long optimalTime = measureTime(optimalPolicies, optimalRequest);
		
		// Average: Middle policy matches (check ~50% before success)
		int middleIndex = Math.max(1, numPolicies / 2);
		Policies averagePolicies = createPoliciesScenario(numPolicies, middleIndex);
		Request averageRequest = createRequest("target");
		long averageTime = measureTime(averagePolicies, averageRequest);
		
		// Worst: Last policy matches (check all policies before success)
		Policies worstPolicies = createPoliciesScenario(numPolicies, numPolicies);
		Request worstRequest = createRequest("target");
		long worstTime = measureTime(worstPolicies, worstRequest);
		
		return new BenchmarkResult(numPolicies, optimalTime, averageTime, worstTime);
	}
	
	private BenchmarkResult benchmarkAttributes(int numAttributes) {
		// Optimal: First attribute matches
		Policies optimalPolicies = createAttributeScenario(numAttributes, 1);
		Request optimalRequest = createAttributeRequest(numAttributes, 1);
		long optimalTime = measureTime(optimalPolicies, optimalRequest);
		
		// Average: Middle attribute matches  
		int midPos = Math.max(1, numAttributes / 2);
		Policies averagePolicies = createAttributeScenario(numAttributes, midPos);
		Request averageRequest = createAttributeRequest(numAttributes, midPos);
		long averageTime = measureTime(averagePolicies, averageRequest);
		
		// Worst: No attribute matches
		Policies worstPolicies = createAttributeScenario(numAttributes, -1);
		Request worstRequest = createAttributeRequest(numAttributes, numAttributes + 1);
		long worstTime = measureTime(worstPolicies, worstRequest);
		
		return new BenchmarkResult(numAttributes, optimalTime, averageTime, worstTime);
	}
	
	private Policies createPoliciesScenario(int numPolicies, int matchingPolicyIndex) {
		Policies policies = new Policies();
		
		for (int i = 1; i <= numPolicies; i++) {
			Attributes partyAttrs = new Attributes()
				.add("id", "provider" + i)
				.add("type", "service");
			
			Attributes resourceAttrs = new Attributes();
			if (i == matchingPolicyIndex) {
				resourceAttrs.add("resource", "target");
			} else {
				resourceAttrs.add("resource", "other" + i);
			}
			
			Rules rules = new Rules().add(new Rule(resourceAttrs));
			policies.add(new Policy(partyAttrs, rules));
		}
		
		return policies;
	}
	
	private Policies createAttributeScenario(int numAttributes, int matchingAttrIndex) {
		Policies policies = new Policies();
		
		Attributes partyAttrs = new Attributes()
			.add("id", "provider")
			.add("type", "service");
		
		Attributes resourceAttrs = new Attributes()
			.add("resource", "target");
		
		for (int i = 1; i <= numAttributes; i++) {
			if (i == matchingAttrIndex) {
				resourceAttrs.add("attr" + i, "match" + i);
			} else {
				resourceAttrs.add("attr" + i, "nomatch" + i);
			}
		}
		
		Rules rules = new Rules().add(new Rule(resourceAttrs));
		policies.add(new Policy(partyAttrs, rules));
		
		return policies;
	}
	
	private Request createRequest(String resourceValue) {
		return new Request(
			index(999),
			new Attributes().add("resource", resourceValue),
			anySuchThat(new Attributes().add("type", "service"))
		);
	}
	
	private Request createAttributeRequest(int numAttributes, int matchingPosition) {
		Attributes requestAttrs = new Attributes()
			.add("resource", "target");
		
		for (int i = 1; i <= numAttributes; i++) {
			if (i == matchingPosition) {
				requestAttrs.add("attr" + i, "match" + i);
			} else {
				requestAttrs.add("attr" + i, "wrongValue" + i);
			}
		}
		
		return new Request(
			index(999),
			requestAttrs,
			anySuchThat(new Attributes().add("type", "service"))
		);
	}
	
	private long measureTime(Policies policies, Request request) {
		long totalTime = 0;
		
		for (int i = 0; i < REPETITIONS; i++) {
			Semantics semantics = new Semantics(policies);
			
			long startTime = System.nanoTime();
			Result result = semantics.evaluate(request);
			long endTime = System.nanoTime();
			
			// Verify that the evaluation actually ran
			if (result == null) {
				throw new RuntimeException("Result should not be null");
			}
			
			totalTime += (endTime - startTime);
		}
		
		return totalTime / REPETITIONS;
	}
	
	private void saveCSV(String filename, List<String[]> data) {
		try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
			for (String[] row : data) {
				writer.println(String.join(",", row));
			}
			System.out.println("Data saved to: " + filename);
		} catch (IOException e) {
			System.err.println("Error saving CSV: " + e.getMessage());
		}
	}
	
	private record BenchmarkResult(int value, long optimalTime, long averageTime, long worstTime) {}
}
