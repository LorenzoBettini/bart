package bart.core.benchmarks;

import static bart.core.Participants.anySuchThat;
import static bart.core.Participants.index;
import static bart.core.Participants.me;
import static bart.core.Participants.requester;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bart.core.AndExchange;
import bart.core.Attributes;
import bart.core.OrExchange;
import bart.core.Policies;
import bart.core.Policy;
import bart.core.Request;
import bart.core.Result;
import bart.core.Rule;
import bart.core.Rules;
import bart.core.SingleExchange;
import bart.core.semantics.Semantics;

/**
 * JUnit-based benchmark tests for the BART project.
 * These tests measure performance characteristics and can be run from IDE.
 * 
 * @author Lorenzo Bettini
 */
class BenchmarkTest {
	
	private static final int REPETITIONS = 3;
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
	
	private Policies policies;
	private Semantics semantics;
	
	@BeforeEach
	void setup() {
		policies = new Policies();
		semantics = new Semantics(policies);
	}
	
	@Test
	void testPolicyCountPerformance() {
		System.out.println("\n=== Policy Count Performance Test ===");
		
		List<PerformanceData> results = new ArrayList<>();
		
		// Test with small increments for detailed analysis
		int[] policyCounts = {1, 5, 10, 20, 50, 100, 200, 500};
		
		for (int numPolicies : policyCounts) {
			PerformanceData data = measurePolicyPerformance(numPolicies);
			results.add(data);
			
			System.out.printf("Policies: %3d | Optimal: %8.2f μs | Average: %8.2f μs | Worst: %8.2f μs | Ratio: %6.2f%n",
				data.metricValue,
				data.optimalTime / 1000.0,
				data.averageTime / 1000.0,
				data.worstTime / 1000.0,
				data.worstTime / (double) Math.max(data.optimalTime, 1));
		}
		
		savePerformanceData("policy_performance.csv", "Policies", results);
		assertPerformanceGrowth(results, "Policy count");
	}
	
	@Test
	void testAttributeCountPerformance() {
		System.out.println("\n=== Attribute Count Performance Test ===");
		
		List<PerformanceData> results = new ArrayList<>();
		
		// Test with attribute count variations
		int[] attributeCounts = {1, 3, 5, 10, 15, 20, 30, 50};
		
		for (int numAttributes : attributeCounts) {
			PerformanceData data = measureAttributePerformance(numAttributes);
			results.add(data);
			
			System.out.printf("Attributes: %2d | Optimal: %8.2f μs | Average: %8.2f μs | Worst: %8.2f μs | Ratio: %6.2f%n",
				data.metricValue,
				data.optimalTime / 1000.0,
				data.averageTime / 1000.0,
				data.worstTime / 1000.0,
				data.worstTime / (double) Math.max(data.optimalTime, 1));
		}
		
		savePerformanceData("attribute_performance.csv", "Attributes", results);
		assertPerformanceGrowth(results, "Attribute count");
	}
	
	@Test
	void testExchangeCountPerformance() {
		System.out.println("\n=== Exchange Count Performance Test ===");
		
		List<PerformanceData> results = new ArrayList<>();
		
		// Test with exchange count variations
		int[] exchangeCounts = {1, 2, 3, 5, 8, 10, 15, 20};
		
		for (int numExchanges : exchangeCounts) {
			PerformanceData data = measureExchangePerformance(numExchanges);
			results.add(data);
			
			System.out.printf("Exchanges: %2d | Optimal: %8.2f μs | Average: %8.2f μs | Worst: %8.2f μs | Ratio: %6.2f%n",
				data.metricValue,
				data.optimalTime / 1000.0,
				data.averageTime / 1000.0,
				data.worstTime / 1000.0,
				data.worstTime / (double) Math.max(data.optimalTime, 1));
		}
		
		savePerformanceData("exchange_performance.csv", "Exchanges", results);
		assertPerformanceGrowth(results, "Exchange count");
	}
	
	@Test
	void testComplexScenarioPerformance() {
		System.out.println("\n=== Complex Scenario Performance Test ===");
		
		// Test realistic complex scenarios with multiple dimensions
		PerformanceData baseline = measureComplexScenario(5, 3, 2);
		PerformanceData medium = measureComplexScenario(20, 8, 5);
		PerformanceData large = measureComplexScenario(50, 15, 10);
		
		System.out.println("Scenario        | Optimal (μs) | Average (μs) | Worst (μs) | Ratio");
		System.out.println("----------------|--------------|--------------|------------|-------");
		
		printComplexScenario("Small (5,3,2)  ", baseline);
		printComplexScenario("Medium (20,8,5)", medium);
		printComplexScenario("Large (50,15,10)", large);
		
		// Verify that performance scales reasonably
		assertTrue(medium.optimalTime > baseline.optimalTime, 
			"Medium scenario should take longer than baseline");
		assertTrue(large.optimalTime > medium.optimalTime, 
			"Large scenario should take longer than medium");
	}
	
	private void printComplexScenario(String label, PerformanceData data) {
		System.out.printf("%-15s | %10.2f | %10.2f | %10.2f | %6.2f%n",
			label,
			data.optimalTime / 1000.0,
			data.averageTime / 1000.0,
			data.worstTime / 1000.0,
			data.worstTime / (double) Math.max(data.optimalTime, 1));
	}
	
	private PerformanceData measurePolicyPerformance(int numPolicies) {
		// Optimal case: first policy matches
		long optimalTime = measureScenario(() -> {
			Policies testPolicies = createTestPolicies(numPolicies, 1);
			Request request = new Request(
				index(999),
				new Attributes().add("resource", "target"),
				anySuchThat(new Attributes().add("type", "provider"))
			);
			return new Semantics(testPolicies).evaluate(request);
		});
		
		// Average case: middle policy matches
		long averageTime = measureScenario(() -> {
			Policies testPolicies = createTestPolicies(numPolicies, Math.max(1, numPolicies / 2));
			Request request = new Request(
				index(999),
				new Attributes().add("resource", "target"),
				anySuchThat(new Attributes().add("type", "provider"))
			);
			return new Semantics(testPolicies).evaluate(request);
		});
		
		// Worst case: no policy matches (all must be evaluated)
		long worstTime = measureScenario(() -> {
			Policies testPolicies = createTestPolicies(numPolicies, -1); // No match
			Request request = new Request(
				index(999),
				new Attributes().add("resource", "nonexistent"),
				anySuchThat(new Attributes().add("type", "provider"))
			);
			return new Semantics(testPolicies).evaluate(request);
		});
		
		return new PerformanceData(numPolicies, optimalTime, averageTime, worstTime);
	}
	
	private PerformanceData measureAttributePerformance(int numAttributes) {
		// Optimal case: first attribute combination matches
		long optimalTime = measureScenario(() -> {
			Policies testPolicies = createAttributeTestPolicies(numAttributes);
			Request request = createAttributeTestRequest(numAttributes, 1);
			return new Semantics(testPolicies).evaluate(request);
		});
		
		// Average case: middle attribute combination matches
		long averageTime = measureScenario(() -> {
			Policies testPolicies = createAttributeTestPolicies(numAttributes);
			Request request = createAttributeTestRequest(numAttributes, Math.max(1, numAttributes / 2));
			return new Semantics(testPolicies).evaluate(request);
		});
		
		// Worst case: no attribute combination matches
		long worstTime = measureScenario(() -> {
			Policies testPolicies = createAttributeTestPolicies(numAttributes);
			Request request = createAttributeTestRequest(numAttributes, -1); // No match
			return new Semantics(testPolicies).evaluate(request);
		});
		
		return new PerformanceData(numAttributes, optimalTime, averageTime, worstTime);
	}
	
	private PerformanceData measureExchangePerformance(int numExchanges) {
		// Optimal case: first exchange succeeds
		long optimalTime = measureScenario(() -> {
			Policies testPolicies = createExchangeTestPolicies(numExchanges, 1);
			Request request = new Request(
				index(999),
				new Attributes().add("resource", "target"),
				anySuchThat(new Attributes().add("type", "provider"))
			);
			return new Semantics(testPolicies).evaluate(request);
		});
		
		// Average case: middle exchange succeeds
		long averageTime = measureScenario(() -> {
			Policies testPolicies = createExchangeTestPolicies(numExchanges, Math.max(1, numExchanges / 2));
			Request request = new Request(
				index(999),
				new Attributes().add("resource", "target"),
				anySuchThat(new Attributes().add("type", "provider"))
			);
			return new Semantics(testPolicies).evaluate(request);
		});
		
		// Worst case: all exchanges fail
		long worstTime = measureScenario(() -> {
			Policies testPolicies = createExchangeTestPolicies(numExchanges, -1);
			Request request = new Request(
				index(999),
				new Attributes().add("resource", "target"),
				anySuchThat(new Attributes().add("type", "provider"))
			);
			return new Semantics(testPolicies).evaluate(request);
		});
		
		return new PerformanceData(numExchanges, optimalTime, averageTime, worstTime);
	}
	
	private PerformanceData measureComplexScenario(int numPolicies, int numAttributes, int numExchanges) {
		// Create complex scenario with all dimensions varying
		long optimalTime = measureScenario(() -> {
			Policies testPolicies = createComplexTestPolicies(numPolicies, numAttributes, numExchanges, true);
			Request request = createComplexTestRequest(numAttributes);
			return new Semantics(testPolicies).evaluate(request);
		});
		
		long averageTime = measureScenario(() -> {
			Policies testPolicies = createComplexTestPolicies(numPolicies, numAttributes, numExchanges, true);
			Request request = createComplexTestRequest(numAttributes);
			return new Semantics(testPolicies).evaluate(request);
		});
		
		long worstTime = measureScenario(() -> {
			Policies testPolicies = createComplexTestPolicies(numPolicies, numAttributes, numExchanges, false);
			Request request = createComplexTestRequest(numAttributes);
			return new Semantics(testPolicies).evaluate(request);
		});
		
		return new PerformanceData(numPolicies * numAttributes * numExchanges, optimalTime, averageTime, worstTime);
	}
	
	private Policies createTestPolicies(int count, int matchingIndex) {
		Policies testPolicies = new Policies();
		
		for (int i = 1; i <= count; i++) {
			Attributes partyAttrs = new Attributes()
				.add("id", "provider" + i)
				.add("type", "provider");
			
			Attributes resourceAttrs = new Attributes();
			if (i == matchingIndex) {
				resourceAttrs.add("resource", "target");
			} else {
				resourceAttrs.add("resource", "other" + i);
			}
			
			Rules rules = new Rules().add(new Rule(resourceAttrs));
			testPolicies.add(new Policy(partyAttrs, rules));
		}
		
		return testPolicies;
	}
	
	private Policies createAttributeTestPolicies(int numAttributes) {
		Policies testPolicies = new Policies();
		
		Attributes partyAttrs = new Attributes()
			.add("id", "provider")
			.add("type", "provider");
		
		Attributes resourceAttrs = new Attributes()
			.add("resource", "target");
		
		// Add many attributes to test matching performance
		for (int i = 1; i <= numAttributes; i++) {
			resourceAttrs.add("attr" + i, "value" + i);
		}
		
		Rules rules = new Rules().add(new Rule(resourceAttrs));
		testPolicies.add(new Policy(partyAttrs, rules));
		
		return testPolicies;
	}
	
	private Request createAttributeTestRequest(int numAttributes, int matchingPosition) {
		Attributes requestAttrs = new Attributes()
			.add("resource", "target");
		
		for (int i = 1; i <= numAttributes; i++) {
			if (i <= matchingPosition && matchingPosition > 0) {
				requestAttrs.add("attr" + i, "value" + i); // This will match
			} else {
				requestAttrs.add("attr" + i, "wrong" + i); // This won't match
			}
		}
		
		return new Request(
			index(999),
			requestAttrs,
			anySuchThat(new Attributes().add("type", "provider"))
		);
	}
	
	private Policies createExchangeTestPolicies(int numExchanges, int successfulExchangeIndex) {
		Policies testPolicies = new Policies();
		
		// Main provider policy with complex exchange
		Attributes providerAttrs = new Attributes()
			.add("id", "provider")
			.add("type", "provider");
		
		Attributes resourceAttrs = new Attributes()
			.add("resource", "target");
		
		bart.core.Exchange exchange = createComplexExchange(numExchanges, successfulExchangeIndex);
		Rules rules = new Rules().add(new Rule(resourceAttrs, exchange));
		testPolicies.add(new Policy(providerAttrs, rules));
		
		// Supporting policies for exchanges
		for (int i = 1; i <= numExchanges; i++) {
			Attributes supportAttrs = new Attributes()
				.add("id", "support" + i)
				.add("type", "support");
			
			Attributes supportResourceAttrs = new Attributes();
			if (i == successfulExchangeIndex) {
				supportResourceAttrs.add("supportResource", "needed" + i);
			} else {
				supportResourceAttrs.add("supportResource", "different" + i);
			}
			
			Rules supportRules = new Rules().add(new Rule(supportResourceAttrs));
			testPolicies.add(new Policy(supportAttrs, supportRules));
		}
		
		return testPolicies;
	}
	
	private bart.core.Exchange createComplexExchange(int numExchanges, int successfulIndex) {
		if (numExchanges == 1) {
			return new SingleExchange(
				me(),
				new Attributes().add("supportResource", "needed1"),
				requester()
			);
		}
		
		List<bart.core.Exchange> exchanges = new ArrayList<>();
		for (int i = 1; i <= numExchanges; i++) {
			exchanges.add(new SingleExchange(
				me(),
				new Attributes().add("supportResource", "needed" + i),
				requester()
			));
		}
		
		// Create OR combination - first success wins
		bart.core.Exchange result = exchanges.get(0);
		for (int i = 1; i < exchanges.size(); i++) {
			result = new OrExchange(result, exchanges.get(i));
		}
		
		return result;
	}
	
	private Policies createComplexTestPolicies(int numPolicies, int numAttributes, int numExchanges, boolean shouldSucceed) {
		Policies testPolicies = new Policies();
		
		for (int p = 1; p <= numPolicies; p++) {
			Attributes partyAttrs = new Attributes()
				.add("id", "provider" + p)
				.add("type", "provider");
			
			Attributes resourceAttrs = new Attributes();
			if (p == 1 && shouldSucceed) {
				resourceAttrs.add("resource", "target");
			} else {
				resourceAttrs.add("resource", "other" + p);
			}
			
			// Add many attributes
			for (int a = 1; a <= numAttributes; a++) {
				resourceAttrs.add("attr" + a, "value" + a);
			}
			
			bart.core.Exchange exchange = null;
			if (numExchanges > 0) {
				exchange = createComplexExchange(numExchanges, shouldSucceed ? 1 : -1);
			}
			
			Rules rules = new Rules().add(new Rule(resourceAttrs, exchange));
			testPolicies.add(new Policy(partyAttrs, rules));
		}
		
		// Add support policies for exchanges
		if (numExchanges > 0) {
			for (int e = 1; e <= numExchanges; e++) {
				Attributes supportAttrs = new Attributes()
					.add("id", "support" + e)
					.add("type", "support");
				
				Attributes supportResourceAttrs = new Attributes();
				if (e == 1 && shouldSucceed) {
					supportResourceAttrs.add("supportResource", "needed" + e);
				} else {
					supportResourceAttrs.add("supportResource", "different" + e);
				}
				
				Rules supportRules = new Rules().add(new Rule(supportResourceAttrs));
				testPolicies.add(new Policy(supportAttrs, supportRules));
			}
		}
		
		return testPolicies;
	}
	
	private Request createComplexTestRequest(int numAttributes) {
		Attributes requestAttrs = new Attributes()
			.add("resource", "target");
		
		for (int i = 1; i <= numAttributes; i++) {
			requestAttrs.add("attr" + i, "value" + i);
		}
		
		return new Request(
			index(999),
			requestAttrs,
			anySuchThat(new Attributes().add("type", "provider"))
		);
	}
	
	private long measureScenario(BenchmarkScenario scenario) {
		long totalTime = 0;
		
		for (int i = 0; i < REPETITIONS; i++) {
			long startTime = System.nanoTime();
			Result result = scenario.execute();
			long endTime = System.nanoTime();
			
			// Verify that the evaluation actually ran
			assertNotNull(result, "Result should not be null");
			
			totalTime += (endTime - startTime);
		}
		
		return totalTime / REPETITIONS;
	}
	
	private void assertPerformanceGrowth(List<PerformanceData> results, String metricName) {
		assertTrue(results.size() > 1, metricName + " should have multiple data points");
		
		// Check that performance generally increases with metric size
		PerformanceData first = results.get(0);
		PerformanceData last = results.get(results.size() - 1);
		
		// Allow some variance due to measurement noise, but expect general growth
		double growthFactor = 0.5; // Allow 50% variance
		assertTrue(last.worstTime >= first.worstTime * growthFactor,
			metricName + " worst case should generally increase with size");
	}
	
	private void savePerformanceData(String filename, String metricName, List<PerformanceData> data) {
		try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
			writer.println(metricName + ",Optimal_ns,Average_ns,Worst_ns,Optimal_us,Average_us,Worst_us");
			
			for (PerformanceData point : data) {
				writer.printf("%d,%d,%d,%d,%.2f,%.2f,%.2f%n",
					point.metricValue,
					point.optimalTime,
					point.averageTime,
					point.worstTime,
					point.optimalTime / 1000.0,
					point.averageTime / 1000.0,
					point.worstTime / 1000.0);
			}
			
			System.out.println("Performance data saved to: " + filename);
		} catch (IOException e) {
			System.err.println("Failed to save performance data: " + e.getMessage());
		}
	}
	
	@FunctionalInterface
	private interface BenchmarkScenario {
		Result execute();
	}
	
	private record PerformanceData(int metricValue, long optimalTime, long averageTime, long worstTime) {}
}
