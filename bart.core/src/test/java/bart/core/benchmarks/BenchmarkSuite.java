package bart.core.benchmarks;

import static bart.core.Participants.anySuchThat;
import static bart.core.Participants.index;
import static bart.core.Participants.me;
import static bart.core.Participants.requester;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
 * Comprehensive benchmarking suite for the BART project.
 * Measures execution time vs various metrics:
 * - Number of policies
 * - Number of attributes
 * - Number of exchanges
 * 
 * Each metric is tested with three scenarios:
 * - Optimal: Success after evaluating minimal elements
 * - Average: Success after evaluating ~50% of elements  
 * - Worst: Denial after evaluating all elements
 * 
 * @author Lorenzo Bettini
 */
public class BenchmarkSuite {
	
	private static final int REPETITIONS = 3;
	private static final String CSV_SEPARATOR = ",";
	
	// Baseline configuration when not varying a specific metric
	private static final int BASELINE_POLICIES = 10;
	private static final int BASELINE_ATTRIBUTES = 5;
	private static final int BASELINE_EXCHANGES = 3;
	
	public static void main(String[] args) {
		BenchmarkSuite suite = new BenchmarkSuite();
		suite.runAllBenchmarks();
	}
	
	public void runAllBenchmarks() {
		System.out.println("BART Performance Benchmarks");
		System.out.println("============================");
		System.out.println();
		
		// Benchmark 1: Number of policies (1-1000, step 100)
		System.out.println("1. Benchmarking number of policies...");
		List<BenchmarkResult> policyResults = benchmarkPolicies();
		printResults("Policies", policyResults);
		saveToCSV("policies_benchmark.csv", "Number of Policies", policyResults);
		
		// Benchmark 2: Number of attributes (1-100, step 10)
		System.out.println("\n2. Benchmarking number of attributes...");
		List<BenchmarkResult> attributeResults = benchmarkAttributes();
		printResults("Attributes", attributeResults);
		saveToCSV("attributes_benchmark.csv", "Number of Attributes", attributeResults);
		
		// Benchmark 3: Number of exchanges (1-100, step 10)
		System.out.println("\n3. Benchmarking number of exchanges...");
		List<BenchmarkResult> exchangeResults = benchmarkExchanges();
		printResults("Exchanges", exchangeResults);
		saveToCSV("exchanges_benchmark.csv", "Number of Exchanges", exchangeResults);
		
		System.out.println("\nBenchmarks completed. CSV files saved for analysis.");
	}
	
	private List<BenchmarkResult> benchmarkPolicies() {
		List<BenchmarkResult> results = new ArrayList<>();
		
		for (int numPolicies = 1; numPolicies <= 1000; numPolicies += 100) {
			// Optimal case: success on first policy
			long optimalTime = measurePolicyBenchmark(numPolicies, ScenarioType.OPTIMAL);
			
			// Average case: success on middle policy
			long averageTime = measurePolicyBenchmark(numPolicies, ScenarioType.AVERAGE);
			
			// Worst case: denial after all policies
			long worstTime = measurePolicyBenchmark(numPolicies, ScenarioType.WORST);
			
			results.add(new BenchmarkResult(numPolicies, optimalTime, averageTime, worstTime));
		}
		
		return results;
	}
	
	private List<BenchmarkResult> benchmarkAttributes() {
		List<BenchmarkResult> results = new ArrayList<>();
		
		for (int numAttributes = 1; numAttributes <= 100; numAttributes += 10) {
			// Optimal case: match on first attribute
			long optimalTime = measureAttributeBenchmark(numAttributes, ScenarioType.OPTIMAL);
			
			// Average case: match on middle attribute
			long averageTime = measureAttributeBenchmark(numAttributes, ScenarioType.AVERAGE);
			
			// Worst case: no match after all attributes
			long worstTime = measureAttributeBenchmark(numAttributes, ScenarioType.WORST);
			
			results.add(new BenchmarkResult(numAttributes, optimalTime, averageTime, worstTime));
		}
		
		return results;
	}
	
	private List<BenchmarkResult> benchmarkExchanges() {
		List<BenchmarkResult> results = new ArrayList<>();
		
		for (int numExchanges = 1; numExchanges <= 100; numExchanges += 10) {
			// Optimal case: success on first exchange
			long optimalTime = measureExchangeBenchmark(numExchanges, ScenarioType.OPTIMAL);
			
			// Average case: success on middle exchange
			long averageTime = measureExchangeBenchmark(numExchanges, ScenarioType.AVERAGE);
			
			// Worst case: failure after all exchanges
			long worstTime = measureExchangeBenchmark(numExchanges, ScenarioType.WORST);
			
			results.add(new BenchmarkResult(numExchanges, optimalTime, averageTime, worstTime));
		}
		
		return results;
	}
	
	private long measurePolicyBenchmark(int numPolicies, ScenarioType scenario) {
		Policies policies = createPoliciesForBenchmark(numPolicies, scenario);
		Request request = createRequestForPolicyBenchmark(scenario);
		
		return measureEvaluation(policies, request);
	}
	
	private long measureAttributeBenchmark(int numAttributes, ScenarioType scenario) {
		Policies policies = createPoliciesWithAttributes(numAttributes, scenario);
		Request request = createRequestForAttributeBenchmark(numAttributes, scenario);
		
		return measureEvaluation(policies, request);
	}
	
	private long measureExchangeBenchmark(int numExchanges, ScenarioType scenario) {
		Policies policies = createPoliciesWithExchanges(numExchanges, scenario);
		Request request = createRequestForExchangeBenchmark(scenario);
		
		return measureEvaluation(policies, request);
	}
	
	private long measureEvaluation(Policies policies, Request request) {
		long totalTime = 0;
		
		// Repeat measurement for statistical significance
		for (int i = 0; i < REPETITIONS; i++) {
			Semantics semantics = new Semantics(policies);
			
			long startTime = System.nanoTime();
			semantics.evaluate(request);
			long endTime = System.nanoTime();
			
			totalTime += (endTime - startTime);
		}
		
		return totalTime / REPETITIONS; // Return average time in nanoseconds
	}
	
	private Policies createPoliciesForBenchmark(int numPolicies, ScenarioType scenario) {
		Policies policies = new Policies();
		
		for (int i = 1; i <= numPolicies; i++) {
			boolean shouldMatch = switch (scenario) {
				case OPTIMAL -> i == 1;
				case AVERAGE -> i == numPolicies / 2;
				case WORST -> false; // No policy matches
			};
			
			policies.add(createPolicyForTest(i, shouldMatch, BASELINE_ATTRIBUTES, BASELINE_EXCHANGES));
		}
		
		return policies;
	}
	
	private Policies createPoliciesWithAttributes(int numAttributes, ScenarioType scenario) {
		Policies policies = new Policies();
		
		for (int i = 1; i <= BASELINE_POLICIES; i++) {
			policies.add(createPolicyForTest(i, i == 1, numAttributes, BASELINE_EXCHANGES));
		}
		
		return policies;
	}
	
	private Policies createPoliciesWithExchanges(int numExchanges, ScenarioType scenario) {
		Policies policies = new Policies();
		
		for (int i = 1; i <= BASELINE_POLICIES; i++) {
			policies.add(createPolicyForTest(i, i == 1, BASELINE_ATTRIBUTES, numExchanges));
		}
		
		return policies;
	}
	
	private Policy createPolicyForTest(int index, boolean shouldMatchRequest, int numAttributes, int numExchanges) {
		// Create party attributes
		Attributes partyAttrs = new Attributes()
			.add("id", "party" + index)
			.add("type", "provider");
		
		for (int i = 1; i <= numAttributes; i++) {
			partyAttrs.add("attr" + i, "value" + i);
		}
		
		// Create rules
		Rules rules = new Rules();
		
		// Create resource attributes for the rule
		Attributes resourceAttrs = new Attributes();
		if (shouldMatchRequest) {
			resourceAttrs.add("resource", "targetResource");
		} else {
			resourceAttrs.add("resource", "otherResource" + index);
		}
		
		for (int i = 1; i <= numAttributes; i++) {
			resourceAttrs.add("reqAttr" + i, "reqValue" + i);
		}
		
		// Create exchanges
		if (numExchanges == 0) {
			rules.add(new Rule(resourceAttrs));
		} else if (numExchanges == 1) {
			rules.add(new Rule(resourceAttrs, new SingleExchange(
				me(),
				new Attributes().add("exchangeResource", "simpleResource"),
				requester()
			)));
		} else {
			// Create complex exchange with AND/OR combinations
			rules.add(new Rule(resourceAttrs, createComplexExchange(numExchanges)));
		}
		
		return new Policy(partyAttrs, rules);
	}
	
	private bart.core.Exchange createComplexExchange(int numExchanges) {
		if (numExchanges <= 1) {
			return new SingleExchange(
				me(),
				new Attributes().add("exchangeResource", "resource1"),
				requester()
			);
		}
		
		List<bart.core.Exchange> exchanges = new ArrayList<>();
		for (int i = 1; i <= numExchanges; i++) {
			exchanges.add(new SingleExchange(
				me(),
				new Attributes().add("exchangeResource", "resource" + i),
				requester()
			));
		}
		
		// Create nested AND/OR combinations
		bart.core.Exchange result = exchanges.get(0);
		for (int i = 1; i < exchanges.size(); i++) {
			if (i % 2 == 1) {
				// Use OR for odd indices
				result = new OrExchange(result, exchanges.get(i));
			} else {
				// Use AND for even indices
				result = new AndExchange(result, exchanges.get(i));
			}
		}
		
		return result;
	}
	
	private Request createRequestForPolicyBenchmark(ScenarioType scenario) {
		return new Request(
			index(999), // Requester not in policies
			new Attributes().add("resource", scenario == ScenarioType.WORST ? "nonExistentResource" : "targetResource"),
			anySuchThat(new Attributes().add("type", "provider"))
		);
	}
	
	private Request createRequestForAttributeBenchmark(int numAttributes, ScenarioType scenario) {
		Attributes requestAttrs = new Attributes();
		requestAttrs.add("resource", "targetResource");
		
		// Add attributes that will match based on scenario
		int matchPosition = switch (scenario) {
			case OPTIMAL -> 1;
			case AVERAGE -> numAttributes / 2;
			case WORST -> numAttributes + 1; // No match
		};
		
		for (int i = 1; i <= numAttributes; i++) {
			if (i == matchPosition) {
				requestAttrs.add("reqAttr" + i, "reqValue" + i); // This will match
			} else {
				requestAttrs.add("reqAttr" + i, "wrongValue" + i); // This won't match
			}
		}
		
		return new Request(
			index(999),
			requestAttrs,
			anySuchThat(new Attributes().add("type", "provider"))
		);
	}
	
	private Request createRequestForExchangeBenchmark(ScenarioType scenario) {
		return new Request(
			index(999),
			new Attributes().add("resource", "targetResource"),
			anySuchThat(new Attributes().add("type", "provider"))
		);
	}
	
	private void printResults(String metricName, List<BenchmarkResult> results) {
		System.out.printf("%-15s %-15s %-15s %-15s%n", metricName, "Optimal (ns)", "Average (ns)", "Worst (ns)");
		System.out.println("-".repeat(75));
		
		for (BenchmarkResult result : results) {
			System.out.printf("%-15d %-15d %-15d %-15d%n", 
				result.metricValue(), 
				result.optimalTime(), 
				result.averageTime(), 
				result.worstTime());
		}
	}
	
	private void saveToCSV(String filename, String metricName, List<BenchmarkResult> results) {
		try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
			// Write header
			writer.println(metricName + CSV_SEPARATOR + "Optimal_ns" + CSV_SEPARATOR + "Average_ns" + CSV_SEPARATOR + "Worst_ns");
			
			// Write data
			for (BenchmarkResult result : results) {
				writer.printf("%d%s%d%s%d%s%d%n",
					result.metricValue(), CSV_SEPARATOR,
					result.optimalTime(), CSV_SEPARATOR,
					result.averageTime(), CSV_SEPARATOR,
					result.worstTime());
			}
			
			System.out.println("Results saved to: " + filename);
		} catch (IOException e) {
			System.err.println("Error saving CSV file: " + e.getMessage());
		}
	}
	
	private enum ScenarioType {
		OPTIMAL, AVERAGE, WORST
	}
	
	private record BenchmarkResult(int metricValue, long optimalTime, long averageTime, long worstTime) {}
}
