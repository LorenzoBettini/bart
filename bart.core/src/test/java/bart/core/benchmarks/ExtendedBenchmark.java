package bart.core.benchmarks;

import static bart.core.Participants.any;
import static bart.core.Participants.index;
import static bart.core.Participants.me;
import static bart.core.Participants.requester;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import bart.core.AndExchange;
import bart.core.Attributes;
import bart.core.Policies;
import bart.core.Policy;
import bart.core.Request;
import bart.core.Result;
import bart.core.Rule;
import bart.core.Rules;
import bart.core.SingleExchange;
import bart.core.semantics.Semantics;

/**
 * Extended benchmark runner for comprehensive BART performance analysis.
 * Tests all three metrics with configurable ranges suitable for research papers.
 * 
 * @author Lorenzo Bettini
 */
public class ExtendedBenchmark {
	
	private static final int REPETITIONS = 3;
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
	
	public static void main(String[] args) {
		ExtendedBenchmark benchmark = new ExtendedBenchmark();
		
		System.out.println("BART Extended Performance Benchmark");
		System.out.println("===================================");
		System.out.println("This benchmark measures execution time vs three key metrics:");
		System.out.println("- Number of policies (1-1000, step 50)");
		System.out.println("- Number of attributes (1-100, step 10)");
		System.out.println("- Number of exchanges (1-100, step 10)");
		System.out.println("Each test uses middle-position matching for consistent results.");
		System.out.println();
		
		// JVM warm-up to avoid cold start bias
		System.out.println("Warming up JVM...");
		benchmark.warmUpJvm();
		System.out.println("Warm-up complete. Starting benchmarks...");
		System.out.println();
		
		String mode = args.length > 0 ? args[0] : "all";
		
		switch (mode.toLowerCase()) {
			case "policies" -> benchmark.runPolicyBenchmark();
			case "attributes" -> benchmark.runAttributeBenchmark();
			case "exchanges" -> benchmark.runExchangeBenchmark();
			case "quick" -> benchmark.runQuickBenchmark();
			default -> benchmark.runAllBenchmarks();
		}
		
		System.out.println("\nBenchmark complete! CSV files generated for analysis.");
	}
	
	/**
	 * Warm up the JVM to avoid cold start bias in benchmark measurements.
	 * Runs several iterations with typical scenarios to trigger JIT compilation.
	 */
	private void warmUpJvm() {
		// Create typical scenarios for warm-up
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
	
	public void runAllBenchmarks() {
		System.out.println("Running complete benchmark suite...");
		System.out.println("This may take several minutes.");
		System.out.println();
		
		runPolicyBenchmark();
		runAttributeBenchmark(); 
		runExchangeBenchmark();
	}
	
	public void runQuickBenchmark() {
		System.out.println("Running quick benchmark for immediate feedback...");
		System.out.println();
		
		runPolicyBenchmarkRange(new int[]{1, 5, 10, 20, 50}, "quick_policies_benchmark.csv");
		runAttributeBenchmarkRange(new int[]{1, 3, 5, 10, 15}, "quick_attributes_benchmark.csv");
		runExchangeBenchmarkRange(new int[]{1, 2, 3, 5, 8}, "quick_exchanges_benchmark.csv");
	}
	
	public void runPolicyBenchmark() {
		System.out.println("=== Policy Count Benchmark ===");
		System.out.println("Testing how execution time scales with number of policies");
		System.out.println("Range: 1-1000 policies, step 50");
		System.out.println();
		
		int[] policyCounts = generateRange(1, 1000, 50);
		runPolicyBenchmarkRange(policyCounts, "policies_benchmark.csv");
	}
	
	public void runAttributeBenchmark() {
		System.out.println("=== Attribute Count Benchmark ===");
		System.out.println("Testing how execution time scales with number of attributes");
		System.out.println("Range: 1-100 attributes, step 10");
		System.out.println();
		
		int[] attributeCounts = generateRange(1, 100, 10);
		runAttributeBenchmarkRange(attributeCounts, "attributes_benchmark.csv");
	}
	
	public void runExchangeBenchmark() {
		System.out.println("=== Exchange Count Benchmark ===");
		System.out.println("Testing how execution time scales with number of exchanges");
		System.out.println("Range: 1-100 exchanges, step 10");
		System.out.println();
		
		int[] exchangeCounts = generateRange(1, 100, 10);
		runExchangeBenchmarkRange(exchangeCounts, "exchanges_benchmark.csv");
	}
	
	private void runPolicyBenchmarkRange(int[] policyCounts, String csvFilename) {
		System.out.printf("%-10s %-15s%n", 
			"Policies", "Time (μs)");
		System.out.println("-".repeat(30));
		
		List<String[]> csvData = new ArrayList<>();
		csvData.add(new String[]{"Policies", "Time_us"});
		
		for (int numPolicies : policyCounts) {
			System.out.printf("Testing %d policies... ", numPolicies);
			
			long executionTime = benchmarkPolicies(numPolicies);
			
			double timeMicros = executionTime / 1000.0;
			
			System.out.printf("done%n");
			System.out.printf("%-10d %-15s%n",
				numPolicies,
				DECIMAL_FORMAT.format(timeMicros));
			
			csvData.add(new String[]{
				String.valueOf(numPolicies),
				String.format("%.2f", timeMicros)
			});
		}
		
		saveCSV(csvFilename, csvData);
		System.out.println();
	}
	
	private void runAttributeBenchmarkRange(int[] attributeCounts, String csvFilename) {
		System.out.printf("%-12s %-15s%n", 
			"Attributes", "Time (μs)");
		System.out.println("-".repeat(32));
		
		List<String[]> csvData = new ArrayList<>();
		csvData.add(new String[]{"Attributes", "Time_us"});
		
		for (int numAttributes : attributeCounts) {
			System.out.printf("Testing %d attributes... ", numAttributes);
			
			long executionTime = benchmarkAttributes(numAttributes);
			
			double timeMicros = executionTime / 1000.0;
			
			System.out.printf("done%n");
			System.out.printf("%-12d %-15s%n",
				numAttributes,
				DECIMAL_FORMAT.format(timeMicros));
			
			csvData.add(new String[]{
				String.valueOf(numAttributes),
				String.format("%.2f", timeMicros)
			});
		}
		
		saveCSV(csvFilename, csvData);
		System.out.println();
	}
	
	private void runExchangeBenchmarkRange(int[] exchangeCounts, String csvFilename) {
		System.out.printf("%-11s %-15s%n", 
			"Exchanges", "Time (μs)");
		System.out.println("-".repeat(31));
		
		List<String[]> csvData = new ArrayList<>();
		csvData.add(new String[]{"Exchanges", "Time_us"});
		
		for (int numExchanges : exchangeCounts) {
			System.out.printf("Testing %d exchanges... ", numExchanges);
			
			long executionTime = benchmarkExchanges(numExchanges);
			
			double timeMicros = executionTime / 1000.0;
			
			System.out.printf("done%n");
			System.out.printf("%-11d %-15s%n",
				numExchanges,
				DECIMAL_FORMAT.format(timeMicros));
			
			csvData.add(new String[]{
				String.valueOf(numExchanges),
				String.format("%.2f", timeMicros)
			});
		}
		
		saveCSV(csvFilename, csvData);
		System.out.println();
	}
	
	private long benchmarkPolicies(int numPolicies) {
		// Always match middle policy for consistent results
		// Since loop starts from 1, middle index should be in range [1, numPolicies]
		int middleIndex = Math.max(1, (numPolicies + 1) / 2);
		Policies policies = createPoliciesScenario(numPolicies, middleIndex);
		Request request = createRequest("target");
		
		// Add extra warm-up for policy benchmark to avoid first-run bias
		for (int i = 0; i < 5; i++) {
			Semantics warmupSemantics = new Semantics(policies);
			warmupSemantics.evaluate(request);
		}
		
		return measureTime(policies, request);
	}
	
	private long benchmarkAttributes(int numAttributes) {
		// Always match middle attribute for consistent results
		int midPos = Math.max(1, numAttributes / 2);
		Policies policies = createAttributeScenario(numAttributes, midPos);
		Request request = createAttributeRequest(numAttributes, midPos);
		
		// Add extra warm-up for attribute benchmark to avoid first-run bias
		for (int i = 0; i < 5; i++) {
			Semantics warmupSemantics = new Semantics(policies);
			warmupSemantics.evaluate(request);
		}
		
		return measureTime(policies, request);
	}
	
	private long benchmarkExchanges(int numExchanges) {
		// Always have middle exchange succeed for consistent results
		int midPos = Math.max(1, numExchanges / 2);
		Policies policies = createExchangeScenario(numExchanges, midPos);
		
		// Request from index 1 (requester) to index 2 (provider) 
		Request request = new Request(
			index(1),  // requester (first policy)
			new Attributes()
				.add("resource", "target")
				.add("scope", "public"),
			index(2)   // provider (second policy)
		);
		
		// Add extra warm-up for exchange benchmark to avoid first-run bias
		for (int i = 0; i < 5; i++) {
			Semantics warmupSemantics = new Semantics(policies);
			warmupSemantics.evaluate(request);
		}
		
		return measureTime(policies, request);
	}
	
	private Policies createPoliciesScenario(int numPolicies, int matchingPolicyIndex) {
		Policies policies = new Policies();
		
		for (int i = 1; i <= numPolicies; i++) {
			Attributes partyAttrs = new Attributes()
				.add("id", "provider" + i)
				.add("type", "service")
				.add("category", "business");
			
			// For the matching policy, create a rule that will match our test request
			if (i == matchingPolicyIndex) {
				// Create rule with exact matching resource attributes
				Attributes resourceAttrs = new Attributes()
					.add("resource", "target")
					.add("scope", "public");
				Rules rules = new Rules().add(new Rule(resourceAttrs));
				policies.add(new Policy(partyAttrs, rules));
			} else {
				// Create rule with different resource attributes (won't match)
				Attributes resourceAttrs = new Attributes()
					.add("resource", "other" + i)
					.add("scope", "public");
				Rules rules = new Rules().add(new Rule(resourceAttrs));
				policies.add(new Policy(partyAttrs, rules));
			}
		}
		
		return policies;
	}
	
	private Policies createAttributeScenario(int numAttributes, int matchingAttrIndex) {
		Policies policies = new Policies();
		
		// Create multiple policies with increasing attribute complexity
		// Each policy has a different set of attributes, making matching more complex
		for (int policyIndex = 1; policyIndex <= numAttributes; policyIndex++) {
			Attributes partyAttrs = new Attributes()
				.add("id", "provider-" + policyIndex)
				.add("type", "service");
			
			Attributes resourceAttrs = new Attributes()
				.add("resource", "target")
				.add("scope", "public")
				.add("category", (policyIndex == matchingAttrIndex) ? "match" : "nomatch")
				.add("priority", "high");
			
			// Add varying number of detail attributes for complexity
			for (int j = 1; j <= policyIndex; j++) {
				resourceAttrs.add("detail" + j, "value" + j);
			}
			
			Rules rules = new Rules().add(new Rule(resourceAttrs));
			policies.add(new Policy(partyAttrs, rules));
		}
		
		return policies;
	}
	
	private Policies createExchangeScenario(int numExchanges, int successExchangeIndex) {
		Policies policies = new Policies();
		
		// First policy (index 1) - the requester that can provide all needed payment resources
		Attributes requesterAttrs = new Attributes()
			.add("id", "requester")
			.add("type", "client");
		
		Rules requesterRules = new Rules();
		// Add rules for all payment resources that might be needed
		for (int i = 1; i <= numExchanges; i++) {
			Attributes paymentAttrs = new Attributes()
				.add("exchangeResource", "payment" + i)
				.add("scope", "public");
			requesterRules.add(new Rule(paymentAttrs));
		}
		
		// Also add the simple "payment" resource for single exchange case
		Attributes simplePaymentAttrs = new Attributes()
			.add("exchangeResource", "payment")
			.add("scope", "public");
		requesterRules.add(new Rule(simplePaymentAttrs));
		
		policies.add(new Policy(requesterAttrs, requesterRules));
		
		// Second policy (index 2) - the provider with exchange requirement
		Attributes providerAttrs = new Attributes()
			.add("id", "provider")
			.add("type", "service");
		
		Attributes resourceAttrs = new Attributes()
			.add("resource", "target")
			.add("scope", "public");
		
		// Complex exchange that scales with numExchanges
		bart.core.Exchange exchange = createTestExchange(numExchanges, successExchangeIndex);
		Rules rules = new Rules().add(new Rule(resourceAttrs, exchange));
		policies.add(new Policy(providerAttrs, rules));
		
		return policies;
	}
	
	private bart.core.Exchange createTestExchange(int numExchanges, int successIndex) {
		// Create exchanges that increase in complexity based on numExchanges
		if (numExchanges == 1) {
			// Simple single exchange
			return new SingleExchange(
				me(),
				new Attributes()
					.add("exchangeResource", "payment")
					.add("scope", "public"),
				requester()
			);
		} else {
			// Create nested AND exchanges that increase evaluation complexity
			bart.core.Exchange rootExchange = new SingleExchange(
				me(),
				new Attributes()
					.add("exchangeResource", "payment1")
					.add("scope", "public"),
				requester()
			);
			
			// Build nested AND structure for complexity
			for (int i = 2; i <= numExchanges; i++) {
				rootExchange = new AndExchange(
					rootExchange,
					new SingleExchange(
						me(),
						new Attributes()
							.add("exchangeResource", "payment" + i)
							.add("scope", "public"),
						requester()
					)
				);
			}
			
			return rootExchange;
		}
	}
	
	private Request createRequest(String resourceValue) {
		return new Request(
			index(999),
			new Attributes()
				.add("resource", resourceValue)
				.add("scope", "public"),  // Match the policy resource attributes
			any(new Attributes().add("type", "service"))
		);
	}
	
	private Request createAttributeRequest(int numAttributes, int matchingPosition) {
		Attributes requestAttrs = new Attributes()
			.add("resource", "target")
			.add("scope", "public")
			.add("category", "match")  // Will match the target policy
			.add("priority", "high");
		
		// Add the same detail attributes that the matching policy has
		for (int j = 1; j <= matchingPosition; j++) {
			requestAttrs.add("detail" + j, "value" + j);
		}
		
		return new Request(
			index(999),
			requestAttrs,
			any(new Attributes().add("type", "service"))
		);
	}
	
	private long measureTime(Policies policies, Request request) {
		long totalTime = 0;
		
		for (int i = 0; i < REPETITIONS; i++) {
			Semantics semantics = new Semantics(policies);
			
			long startTime = System.nanoTime();
			Result result = semantics.evaluate(request);
			long endTime = System.nanoTime();
			
			// Verify that the evaluation succeeded (request was permitted)
			if (result == null || !result.isPermitted()) {
				throw new RuntimeException("Benchmark failed: request should be permitted but was " + 
					(result == null ? "null" : "denied"));
			}
			
			totalTime += (endTime - startTime);
		}
		
		return totalTime / REPETITIONS;
	}
	
	private int[] generateRange(int start, int end, int step) {
		List<Integer> values = new ArrayList<>();
		for (int i = start; i <= end; i += step) {
			values.add(i);
		}
		return values.stream().mapToInt(Integer::intValue).toArray();
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
}
