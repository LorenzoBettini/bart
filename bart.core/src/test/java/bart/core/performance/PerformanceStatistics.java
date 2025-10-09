package bart.core.performance;

import static bart.core.Participants.any;
import static bart.core.Participants.index;
import static bart.core.Participants.me;
import static bart.core.Participants.requester;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;

import bart.core.AndExchange;
import bart.core.Attributes;
import bart.core.Exchange;
import bart.core.Policies;
import bart.core.Policy;
import bart.core.Request;
import bart.core.Result;
import bart.core.Rule;
import bart.core.Rules;
import bart.core.SingleExchange;
import bart.core.semantics.Semantics;

/**
 * Performance statistics for the BART project.
 * 
 * These tests measure how execution time increases with:
 * - Number of policies
 * - Number of attributes
 * - Number of exchanges
 * 
 * Each metric is measured independently with other factors held constant.
 * 
 * @author Lorenzo Bettini
 */
public class PerformanceStatistics {

	// ==================== CONFIGURATION ====================
	
	// Baseline Configuration (constant values for independent measurements)
	private static final int BASELINE_NUM_POLICIES = 10;
	private static final int BASELINE_NUM_ATTRIBUTES = 5;
	private static final int BASELINE_NUM_EXCHANGES = 3;
	
	// Measurement Ranges and Steps
	// Using larger steps to see clearer performance differences
	// Sequences: Policies 100,1000,...,10000; Attributes 10,100,...,1000; Exchanges 1,10,...,100
	private static final int POLICIES_MIN = 1000;
	private static final int POLICIES_MAX = 10000;
	private static final int POLICIES_STEP = 1000;
	
	private static final int ATTRIBUTES_MIN = 100;
	private static final int ATTRIBUTES_MAX = 1000;
	private static final int ATTRIBUTES_STEP = 100;
	
	private static final int EXCHANGES_MIN = 10;
	private static final int EXCHANGES_MAX = 100;
	private static final int EXCHANGES_STEP = 10;
	
	// Repetitions and Warm-up
	private static final int REPETITIONS = 100;  // More repetitions for statistical significance
	private static final int WARMUP_ITERATIONS = 20;
	
	public static void main(String[] args) {
		PerformanceStatistics tests = new PerformanceStatistics();
		
		System.out.println("=".repeat(80));
		System.out.println("BART Performance Tests");
		System.out.println("=".repeat(80));
		System.out.println();
		
		// Warm up the JVM
		System.out.println("Warming up JVM with " + WARMUP_ITERATIONS + " iterations...");
		tests.warmUp();
		System.out.println("Warm-up complete.\n");
		
		// Run performance tests
		System.out.println("Running performance tests...\n");
		
		tests.testPoliciesPerformance();
		System.out.println();
		
		tests.testAttributesPerformance();
		System.out.println();
		
		tests.testExchangesPerformance();
		System.out.println();
		
		System.out.println("=".repeat(80));
		System.out.println("Performance tests completed.");
		System.out.println("=".repeat(80));
	}
	
	private void warmUp() {
		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			// Create a simple scenario with baseline configuration
			Policies policies = createPoliciesForPolicyTest(BASELINE_NUM_POLICIES, BASELINE_NUM_ATTRIBUTES);
			Semantics semantics = new Semantics(policies);
			Request request = createRequestForPolicyTest();
			
			// Execute the request
			semantics.evaluate(request);
		}
	}
	
	public void testPoliciesPerformance() {
		System.out.println("-".repeat(80));
		System.out.println("Performance Test: Number of Policies");
		System.out.println("-".repeat(80));
		System.out.println("Configuration:");
		System.out.println("  - Attributes per party: " + BASELINE_NUM_ATTRIBUTES + " (constant)");
		System.out.println("  - Exchanges per rule: " + BASELINE_NUM_EXCHANGES + " (constant)");
		System.out.println("  - Policies sequence: 100, " + POLICIES_MIN + " to " + POLICIES_MAX + " (step: " + POLICIES_STEP + ")");
		System.out.println("  - Repetitions: " + REPETITIONS);
		System.out.println();
		
		printTableHeader();
		
		// Measure with 100 policies first
		measureSinglePolicyCount(100);
		
		// Then measure from POLICIES_MIN to POLICIES_MAX
		for (int numPolicies = POLICIES_MIN; numPolicies <= POLICIES_MAX; numPolicies += POLICIES_STEP) {
			measureSinglePolicyCount(numPolicies);
		}
		
		System.out.println("-".repeat(80));
	}
	
	private void measureSinglePolicyCount(int numPolicies) {
		// Create test scenario ONCE outside the measurement loop
		Policies policies = createPoliciesForPolicyTest(numPolicies, BASELINE_NUM_ATTRIBUTES);
		Semantics semantics = new Semantics(policies);
		Request request = createRequestForPolicyTest();
		
		// Verify once that the scenario works
		Result testResult = semantics.evaluate(request);
		assertTrue(testResult.isPermitted(), "Request should be permitted");
		
		List<Long> measurements = new ArrayList<>();
		
		// Now measure ONLY the evaluation time
		for (int rep = 0; rep < REPETITIONS; rep++) {
			long startTime = System.nanoTime();
			semantics.evaluate(request);
			long endTime = System.nanoTime();
			
			measurements.add(endTime - startTime);
		}
		
		printStatistics(numPolicies, measurements);
	}
	
	/**
	 * Creates policies for the policy count test.
	 * Only the LAST policy will match the request.
	 */
	private Policies createPoliciesForPolicyTest(int numPolicies, int numAttributes) {
		Policies policies = new Policies();
		
		// Add the requester's policy (index 1) - this will NOT be evaluated as it's the requester
		Attributes requesterAttrs = new Attributes()
			.add("name", "Requester")
			.add("role", "User");
		for (int a = 0; a < numAttributes - 2; a++) {
			requesterAttrs.add("attr" + a, "value" + a);
		}
		policies.add(new Policy(requesterAttrs, new Rules().add(new Rule())));
		
		// Add policies that won't match (party attributes don't match the request's "from")
		for (int i = 2; i < numPolicies; i++) {
			Attributes partyAttrs = new Attributes()
				.add("name", "Party" + i)
				.add("role", "NonTargetProvider");
			
			// Add additional attributes
			for (int a = 0; a < numAttributes - 2; a++) {
				partyAttrs.add("attr" + a, "value" + a);
			}
			
			// These policies have the correct resource but won't be selected
			// because their party attributes don't match
			Attributes resourceAttrs = new Attributes()
				.add("resource/type", "target");
			
			policies.add(new Policy(
				partyAttrs,
				new Rules().add(new Rule(resourceAttrs))
			));
		}
		
		// Add the LAST policy that will match (both party and resource)
		Attributes lastPartyAttrs = new Attributes()
			.add("name", "MatchingParty")
			.add("role", "TargetProvider");
		
		// Add additional attributes
		for (int a = 0; a < numAttributes - 2; a++) {
			lastPartyAttrs.add("attr" + a, "value" + a);
		}
		
		// Resource that will match
		Attributes matchingResourceAttrs = new Attributes()
			.add("resource/type", "target");
		
		// No exchange needed for policy count test - we're measuring policy selection overhead
		policies.add(new Policy(
			lastPartyAttrs,
			new Rules().add(new Rule(matchingResourceAttrs))
		));
		
		return policies;
	}
	
	/**
	 * Creates a request that will match only the last policy.
	 */
	private Request createRequestForPolicyTest() {
		return new Request(
			index(1), // Requester (first policy)
			new Attributes().add("resource/type", "target"),
			any(new Attributes()
				.add("name", "MatchingParty")
				.add("role", "TargetProvider"))
		);
	}
	
	public void testAttributesPerformance() {
		System.out.println("-".repeat(80));
		System.out.println("Performance Test: Number of Attributes");
		System.out.println("-".repeat(80));
		System.out.println("Configuration:");
		System.out.println("  - Number of policies: " + BASELINE_NUM_POLICIES + " (constant)");
		System.out.println("  - Exchanges per rule: " + BASELINE_NUM_EXCHANGES + " (constant)");
		System.out.println("  - Attributes sequence: 10, " + ATTRIBUTES_MIN + " to " + ATTRIBUTES_MAX + " (step: " + ATTRIBUTES_STEP + ")");
		System.out.println("  - Repetitions: " + REPETITIONS);
		System.out.println();
		
		printTableHeader();
		
		// Measure with 10 attributes first
		measureSingleAttributeCount(10);
		
		// Then measure from ATTRIBUTES_MIN to ATTRIBUTES_MAX
		for (int numAttributes = ATTRIBUTES_MIN; numAttributes <= ATTRIBUTES_MAX; numAttributes += ATTRIBUTES_STEP) {
			measureSingleAttributeCount(numAttributes);
		}
		
		System.out.println("-".repeat(80));
	}
	
	private void measureSingleAttributeCount(int numAttributes) {
		// Create test scenario ONCE outside the measurement loop
		Policies policies = createPoliciesForAttributeTest(BASELINE_NUM_POLICIES, numAttributes);
		Semantics semantics = new Semantics(policies);
		Request request = createRequestForAttributeTest(numAttributes);
		
		// Verify once that the scenario works
		Result testResult = semantics.evaluate(request);
		assertTrue(testResult.isPermitted(), "Request should be permitted");
		
		List<Long> measurements = new ArrayList<>();
		
		// Now measure ONLY the evaluation time
		for (int rep = 0; rep < REPETITIONS; rep++) {
			long startTime = System.nanoTime();
			semantics.evaluate(request);
			long endTime = System.nanoTime();
			
			measurements.add(endTime - startTime);
		}
		
		printStatistics(numAttributes, measurements);
	}
	
	/**
	 * Creates policies for the attribute count test.
	 * Each party will have the specified number of attributes.
	 */
	private Policies createPoliciesForAttributeTest(int numPolicies, int numAttributes) {
		Policies policies = new Policies();
		
		// Add the requester's policy (index 1)
		Attributes requesterAttrs = new Attributes()
			.add("name", "Requester")
			.add("role", "User");
		for (int a = 2; a < numAttributes; a++) {
			requesterAttrs.add("attr" + a, "value" + a);
		}
		policies.add(new Policy(requesterAttrs, new Rules().add(new Rule())));
		
		// Add policies that won't match (party attributes don't match)
		for (int i = 2; i < numPolicies; i++) {
			Attributes partyAttrs = new Attributes()
				.add("name", "Party" + i)
				.add("role", "NonTargetProvider");
			
			// Add variable number of attributes
			for (int a = 2; a < numAttributes; a++) {
				partyAttrs.add("attr" + a, "value" + a);
			}
			
			// Resource attributes
			Attributes resourceAttrs = new Attributes()
				.add("resource/type", "target");
			
			policies.add(new Policy(
				partyAttrs,
				new Rules().add(new Rule(resourceAttrs))
			));
		}
		
		// Add the LAST policy that will match
		Attributes lastPartyAttrs = new Attributes()
			.add("name", "MatchingParty")
			.add("role", "TargetProvider");
		
		// Add variable number of attributes
		for (int a = 2; a < numAttributes; a++) {
			lastPartyAttrs.add("attr" + a, "value" + a);
		}
		
		// Resource that will match
		Attributes matchingResourceAttrs = new Attributes()
			.add("resource/type", "target");
		
		// No exchange needed for attribute count test - we're measuring attribute matching overhead
		policies.add(new Policy(
			lastPartyAttrs,
			new Rules().add(new Rule(matchingResourceAttrs))
		));
		
		return policies;
	}
	
	/**
	 * Creates a request for the attribute test.
	 */
	private Request createRequestForAttributeTest(int numAttributes) {
		Attributes fromAttrs = new Attributes()
			.add("name", "MatchingParty")
			.add("role", "TargetProvider");
		
		// Add matching attributes
		for (int a = 2; a < numAttributes; a++) {
			fromAttrs.add("attr" + a, "value" + a);
		}
		
		return new Request(
			index(1), // Requester
			new Attributes().add("resource/type", "target"),
			any(fromAttrs)
		);
	}
	
	public void testExchangesPerformance() {
		System.out.println("-".repeat(80));
		System.out.println("Performance Test: Number of Exchanges");
		System.out.println("-".repeat(80));
		System.out.println("Configuration:");
		System.out.println("  - Number of policies: " + BASELINE_NUM_POLICIES + " (constant)");
		System.out.println("  - Attributes per party: " + BASELINE_NUM_ATTRIBUTES + " (constant)");
		System.out.println("  - Exchanges sequence: 1, " + EXCHANGES_MIN + " to " + EXCHANGES_MAX + " (step: " + EXCHANGES_STEP + ")");
		System.out.println("  - Exchange type: AND chain");
		System.out.println("  - Repetitions: " + REPETITIONS);
		System.out.println();
		
		printTableHeader();
		
		// Measure with 1 exchange first
		measureSingleExchangeCount(1);
		
		// Then measure from EXCHANGES_MIN to EXCHANGES_MAX
		for (int numExchanges = EXCHANGES_MIN; numExchanges <= EXCHANGES_MAX; numExchanges += EXCHANGES_STEP) {
			measureSingleExchangeCount(numExchanges);
		}
		
		System.out.println("-".repeat(80));
	}
	
	private void measureSingleExchangeCount(int numExchanges) {
		// Create test scenario ONCE outside the measurement loop
		Policies policies = createPoliciesForExchangeTest(BASELINE_NUM_POLICIES, numExchanges);
		Semantics semantics = new Semantics(policies);
		Request request = createRequestForExchangeTest();
		
		// Verify once that the scenario works
		Result testResult = semantics.evaluate(request);
		assertTrue(testResult.isPermitted(), "Request should be permitted");
		
		List<Long> measurements = new ArrayList<>();
		
		// Now measure ONLY the evaluation time
		for (int rep = 0; rep < REPETITIONS; rep++) {
			long startTime = System.nanoTime();
			semantics.evaluate(request);
			long endTime = System.nanoTime();
			
			measurements.add(endTime - startTime);
		}
		
		printStatistics(numExchanges, measurements);
	}
	
	/**
	 * Creates policies for the exchange count test.
	 */
	private Policies createPoliciesForExchangeTest(int numPolicies, int numExchanges) {
		Policies policies = new Policies();
		
		// Add the requester's policy (index 1) with rules to satisfy exchanges
		Attributes requesterAttrs = new Attributes()
			.add("name", "Requester")
			.add("role", "User");
		for (int a = 0; a < BASELINE_NUM_ATTRIBUTES - 2; a++) {
			requesterAttrs.add("attr" + a, "value" + a);
		}
		
		// Add rules to satisfy all exchange requests (for resources resource0, resource1, etc.)
		Rules requesterRules = new Rules();
		for (int i = 0; i < EXCHANGES_MAX; i++) {
			requesterRules.add(new Rule(
				new Attributes().add("exchange/type", "resource" + i)
			));
		}
		policies.add(new Policy(requesterAttrs, requesterRules));
		
		// Add policies that won't match (party attributes don't match)
		for (int i = 2; i < numPolicies; i++) {
			Attributes partyAttrs = new Attributes()
				.add("name", "Party" + i)
				.add("role", "NonTargetProvider");
			
			// Add additional attributes
			for (int a = 0; a < BASELINE_NUM_ATTRIBUTES - 2; a++) {
				partyAttrs.add("attr" + a, "value" + a);
			}
			
			// Resource attributes
			Attributes resourceAttrs = new Attributes()
				.add("resource/type", "target");
			
			policies.add(new Policy(
				partyAttrs,
				new Rules().add(new Rule(resourceAttrs))
			));
		}
		
		// Add the LAST policy that will match with variable exchanges
		Attributes lastPartyAttrs = new Attributes()
			.add("name", "MatchingParty")
			.add("role", "TargetProvider");
		
		// Add additional attributes
		for (int a = 0; a < BASELINE_NUM_ATTRIBUTES - 2; a++) {
			lastPartyAttrs.add("attr" + a, "value" + a);
		}
		
		// Resource that will match
		Attributes matchingResourceAttrs = new Attributes()
			.add("resource/type", "target");
		
		// Create exchanges with varying count
		Exchange exchange = createAndExchangeChain(numExchanges);
		
		policies.add(new Policy(
			lastPartyAttrs,
			new Rules().add(new Rule(matchingResourceAttrs, exchange))
		));
		
		return policies;
	}
	
	/**
	 * Creates a request for the exchange test.
	 */
	private Request createRequestForExchangeTest() {
		return new Request(
			index(1), // Requester
			new Attributes().add("resource/type", "target"),
			any(new Attributes()
				.add("name", "MatchingParty")
				.add("role", "TargetProvider"))
		);
	}
	
	/**
	 * Creates a chain of AND exchanges.
	 * Each exchange requests a simple resource from the requester.
	 */
	private Exchange createAndExchangeChain(int count) {
		if (count <= 0) {
			return null;
		}
		
		if (count == 1) {
			return new SingleExchange(
				me(),
				new Attributes().add("exchange/type", "resource0"),
				requester()
			);
		}
		
		// Build AND chain
		Exchange current = new SingleExchange(
			me(),
			new Attributes().add("exchange/type", "resource0"),
			requester()
		);
		
		for (int i = 1; i < count; i++) {
			Exchange next = new SingleExchange(
				me(),
				new Attributes().add("exchange/type", "resource" + i),
				requester()
			);
			current = new AndExchange(current, next);
		}
		
		return current;
	}
	
	private void printTableHeader() {
		System.out.println(String.format("%-15s %-20s %-20s %-20s %-20s",
			"Metric Value", "Avg Time (ms)", "Min Time (ms)", "Max Time (ms)", "Std Dev (ms)"));
		System.out.println("-".repeat(80));
	}
	
	private void printStatistics(int metricValue, List<Long> measurements) {
		DoubleSummaryStatistics stats = measurements.stream()
			.mapToDouble(Long::doubleValue)
			.summaryStatistics();
		
		double avgNanos = stats.getAverage();
		double minNanos = stats.getMin();
		double maxNanos = stats.getMax();
		
		// Calculate standard deviation
		double mean = avgNanos;
		double variance = measurements.stream()
			.mapToDouble(Long::doubleValue)
			.map(d -> Math.pow(d - mean, 2))
			.average()
			.orElse(0.0);
		double stdDevNanos = Math.sqrt(variance);
		
		// Convert to milliseconds for readability
		double avgMs = avgNanos / 1_000_000.0;
		double minMs = minNanos / 1_000_000.0;
		double maxMs = maxNanos / 1_000_000.0;
		double stdDevMs = stdDevNanos / 1_000_000.0;
		
		System.out.println(String.format("%-15d %-20.6f %-20.6f %-20.6f %-20.6f",
			metricValue, avgMs, minMs, maxMs, stdDevMs));
	}
}
