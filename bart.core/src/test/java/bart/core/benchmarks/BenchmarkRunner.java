package bart.core.benchmarks;

import static bart.core.Participants.anySuchThat;
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
import bart.core.OrExchange;
import bart.core.Policies;
import bart.core.Policy;
import bart.core.Request;
import bart.core.Rule;
import bart.core.Rules;
import bart.core.SingleExchange;
import bart.core.semantics.Semantics;

/**
 * Quick benchmark runner for testing individual metrics or running focused tests.
 * This complements the full BenchmarkSuite with more targeted and faster executions.
 * 
 * @author Lorenzo Bettini
 */
public class BenchmarkRunner {
	
	private static final int REPETITIONS = 3;
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
	
	public static void main(String[] args) {
		BenchmarkRunner runner = new BenchmarkRunner();
		
		if (args.length > 0) {
			switch (args[0].toLowerCase()) {
				case "policies" -> runner.runPolicyBenchmark();
				case "attributes" -> runner.runAttributeBenchmark();
				case "exchanges" -> runner.runExchangeBenchmark();
				case "quick" -> runner.runQuickTest();
				default -> runner.runAllBenchmarks();
			}
		} else {
			runner.runQuickTest();
		}
	}
	
	/**
	 * Quick test with smaller ranges for rapid feedback
	 */
	public void runQuickTest() {
		System.out.println("BART Quick Performance Test");
		System.out.println("===========================");
		System.out.println();
		
		// Test with smaller ranges for quick feedback
		runPolicyBenchmarkRange(1, 100, 20);
		runAttributeBenchmarkRange(1, 20, 5);
		runExchangeBenchmarkRange(1, 20, 5);
	}
	
	public void runAllBenchmarks() {
		System.out.println("BART Complete Performance Benchmarks");
		System.out.println("====================================");
		System.out.println();
		
		runPolicyBenchmark();
		runAttributeBenchmark();
		runExchangeBenchmark();
	}
	
	public void runPolicyBenchmark() {
		System.out.println("Policy Count Benchmark (1-1000, step 100)");
		runPolicyBenchmarkRange(1, 1000, 100);
	}
	
	public void runAttributeBenchmark() {
		System.out.println("Attribute Count Benchmark (1-100, step 10)");
		runAttributeBenchmarkRange(1, 100, 10);
	}
	
	public void runExchangeBenchmark() {
		System.out.println("Exchange Count Benchmark (1-100, step 10)");
		runExchangeBenchmarkRange(1, 100, 10);
	}
	
	private void runPolicyBenchmarkRange(int start, int end, int step) {
		System.out.println("Benchmarking policy count...");
		System.out.printf("%-10s %-15s %-15s %-15s %-15s%n", 
			"Policies", "Optimal (μs)", "Average (μs)", "Worst (μs)", "Ratio (W/O)");
		System.out.println("-".repeat(85));
		
		List<String[]> csvData = new ArrayList<>();
		csvData.add(new String[]{"Policies", "Optimal_us", "Average_us", "Worst_us", "Ratio_Worst_Optimal"});
		
		for (int numPolicies = start; numPolicies <= end; numPolicies += step) {
			BenchmarkResult result = benchmarkPolicies(numPolicies);
			
			double optimalMicros = result.optimalTime() / 1000.0;
			double averageMicros = result.averageTime() / 1000.0;
			double worstMicros = result.worstTime() / 1000.0;
			double ratio = worstMicros / Math.max(optimalMicros, 1);
			
			System.out.printf("%-10d %-15s %-15s %-15s %-15s%n",
				numPolicies,
				DECIMAL_FORMAT.format(optimalMicros),
				DECIMAL_FORMAT.format(averageMicros),
				DECIMAL_FORMAT.format(worstMicros),
				DECIMAL_FORMAT.format(ratio));
			
			csvData.add(new String[]{
				String.valueOf(numPolicies),
				String.format("%.2f", optimalMicros),
				String.format("%.2f", averageMicros),
				String.format("%.2f", worstMicros),
				String.format("%.2f", ratio)
			});
		}
		
		saveCSV("policies_benchmark.csv", csvData);
		System.out.println();
	}
	
	private void runAttributeBenchmarkRange(int start, int end, int step) {
		System.out.println("Benchmarking attribute count...");
		System.out.printf("%-12s %-15s %-15s %-15s %-15s%n", 
			"Attributes", "Optimal (μs)", "Average (μs)", "Worst (μs)", "Ratio (W/O)");
		System.out.println("-".repeat(87));
		
		List<String[]> csvData = new ArrayList<>();
		csvData.add(new String[]{"Attributes", "Optimal_us", "Average_us", "Worst_us", "Ratio_Worst_Optimal"});
		
		for (int numAttributes = start; numAttributes <= end; numAttributes += step) {
			BenchmarkResult result = benchmarkAttributes(numAttributes);
			
			double optimalMicros = result.optimalTime() / 1000.0;
			double averageMicros = result.averageTime() / 1000.0;
			double worstMicros = result.worstTime() / 1000.0;
			double ratio = worstMicros / Math.max(optimalMicros, 1);
			
			System.out.printf("%-12d %-15s %-15s %-15s %-15s%n",
				numAttributes,
				DECIMAL_FORMAT.format(optimalMicros),
				DECIMAL_FORMAT.format(averageMicros),
				DECIMAL_FORMAT.format(worstMicros),
				DECIMAL_FORMAT.format(ratio));
			
			csvData.add(new String[]{
				String.valueOf(numAttributes),
				String.format("%.2f", optimalMicros),
				String.format("%.2f", averageMicros),
				String.format("%.2f", worstMicros),
				String.format("%.2f", ratio)
			});
		}
		
		saveCSV("attributes_benchmark.csv", csvData);
		System.out.println();
	}
	
	private void runExchangeBenchmarkRange(int start, int end, int step) {
		System.out.println("Benchmarking exchange count...");
		System.out.printf("%-11s %-15s %-15s %-15s %-15s%n", 
			"Exchanges", "Optimal (μs)", "Average (μs)", "Worst (μs)", "Ratio (W/O)");
		System.out.println("-".repeat(86));
		
		List<String[]> csvData = new ArrayList<>();
		csvData.add(new String[]{"Exchanges", "Optimal_us", "Average_us", "Worst_us", "Ratio_Worst_Optimal"});
		
		for (int numExchanges = start; numExchanges <= end; numExchanges += step) {
			BenchmarkResult result = benchmarkExchanges(numExchanges);
			
			double optimalMicros = result.optimalTime() / 1000.0;
			double averageMicros = result.averageTime() / 1000.0;
			double worstMicros = result.worstTime() / 1000.0;
			double ratio = worstMicros / Math.max(optimalMicros, 1);
			
			System.out.printf("%-11d %-15s %-15s %-15s %-15s%n",
				numExchanges,
				DECIMAL_FORMAT.format(optimalMicros),
				DECIMAL_FORMAT.format(averageMicros),
				DECIMAL_FORMAT.format(worstMicros),
				DECIMAL_FORMAT.format(ratio));
			
			csvData.add(new String[]{
				String.valueOf(numExchanges),
				String.format("%.2f", optimalMicros),
				String.format("%.2f", averageMicros),
				String.format("%.2f", worstMicros),
				String.format("%.2f", ratio)
			});
		}
		
		saveCSV("exchanges_benchmark.csv", csvData);
		System.out.println();
	}
	
	private BenchmarkResult benchmarkPolicies(int numPolicies) {
		// Optimal: First policy matches
		Policies optimalPolicies = createPoliciesScenario(numPolicies, 1);
		Request optimalRequest = createRequest("target");
		long optimalTime = measureTime(optimalPolicies, optimalRequest);
		
		// Average: Middle policy matches
		Policies averagePolicies = createPoliciesScenario(numPolicies, Math.max(1, numPolicies / 2));
		Request averageRequest = createRequest("target");
		long averageTime = measureTime(averagePolicies, averageRequest);
		
		// Worst: No policy matches
		Policies worstPolicies = createPoliciesScenario(numPolicies, -1);
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
	
	private BenchmarkResult benchmarkExchanges(int numExchanges) {
		// Optimal: First exchange succeeds
		Policies optimalPolicies = createExchangeScenario(numExchanges, 1);
		Request optimalRequest = createRequest("target");
		long optimalTime = measureTime(optimalPolicies, optimalRequest);
		
		// Average: Middle exchange succeeds
		int midPos = Math.max(1, numExchanges / 2);
		Policies averagePolicies = createExchangeScenario(numExchanges, midPos);
		Request averageRequest = createRequest("target");
		long averageTime = measureTime(averagePolicies, averageRequest);
		
		// Worst: All exchanges fail
		Policies worstPolicies = createExchangeScenario(numExchanges, -1);
		Request worstRequest = createRequest("target");
		long worstTime = measureTime(worstPolicies, worstRequest);
		
		return new BenchmarkResult(numExchanges, optimalTime, averageTime, worstTime);
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
	
	private Policies createExchangeScenario(int numExchanges, int successExchangeIndex) {
		Policies policies = new Policies();
		
		// Create provider policy
		Attributes providerAttrs = new Attributes()
			.add("id", "provider")
			.add("type", "service");
		
		Attributes resourceAttrs = new Attributes()
			.add("resource", "target");
		
		bart.core.Exchange exchange = createTestExchange(numExchanges, successExchangeIndex);
		Rules rules = new Rules().add(new Rule(resourceAttrs, exchange));
		policies.add(new Policy(providerAttrs, rules));
		
		// Create supporting policies for exchanges
		for (int i = 1; i <= numExchanges; i++) {
			Attributes supportAttrs = new Attributes()
				.add("id", "support" + i)
				.add("type", "support");
			
			Attributes supportResource = new Attributes();
			if (i == successExchangeIndex) {
				supportResource.add("supportResource", "exchange" + i);
			} else {
				supportResource.add("supportResource", "noMatch" + i);
			}
			
			Rules supportRules = new Rules().add(new Rule(supportResource));
			policies.add(new Policy(supportAttrs, supportRules));
		}
		
		return policies;
	}
	
	private bart.core.Exchange createTestExchange(int numExchanges, int successIndex) {
		if (numExchanges == 1) {
			return new SingleExchange(
				me(),
				new Attributes().add("supportResource", "exchange1"),
				requester()
			);
		}
		
		List<bart.core.Exchange> exchanges = new ArrayList<>();
		for (int i = 1; i <= numExchanges; i++) {
			exchanges.add(new SingleExchange(
				me(),
				new Attributes().add("supportResource", "exchange" + i),
				requester() // Use requester instead of index
			));
		}
		
		// Combine with OR - first successful exchange wins
		bart.core.Exchange result = exchanges.get(0);
		for (int i = 1; i < exchanges.size(); i++) {
			result = new OrExchange(result, exchanges.get(i));
		}
		
		return result;
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
			semantics.evaluate(request);
			long endTime = System.nanoTime();
			
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
