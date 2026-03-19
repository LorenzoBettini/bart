package bart.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * An ordered, 1-based indexed collection of {@link Policy} objects.
 * <p>
 * Parties in Bart are numbered starting from {@code 1}. The order in which
 * policies are added determines their index:
 * the first {@link #add(Policy) add} call creates party 1, the second creates
 * party 2, etc.
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * var policies = new Policies()
 *     .add(new Policy(new Attributes().add("name", "Alice"), new Rules()))
 *     .add(new Policy(new Attributes().add("name", "Bob"),   new Rules()));
 * Policy alice = policies.getByIndex(1); // Alice
 * Policy bob   = policies.getByIndex(2); // Bob
 * }
 * </p>
 *
 * @author Lorenzo Bettini
 *
 */
public class Policies {

	/**
	 * Associates a 1-based index with its {@link Policy}.
	 *
	 * @param index  the 1-based position of the policy in the collection
	 * @param policy the policy at that position
	 */
	public static record PolicyData(int index, Policy policy) {

	}

	private List<Policy> collection = new ArrayList<>();

	/**
	 * Appends a policy to the collection, assigning it the next available index.
	 *
	 * @param policy the policy to add
	 * @return {@code this} to allow fluent chaining
	 */
	public Policies add(Policy policy) {
		collection.add(policy);
		return this;
	}

	/**
	 * Returns a stream of {@link PolicyData} records, each pairing a policy with
	 * its 1-based index.
	 *
	 * @return a sequential stream of policy data
	 */
	public Stream<PolicyData> getPolicyData() {
		return IntStream.range(0, collection.size())
			.mapToObj(i -> new PolicyData(i + 1, collection.get(i)));
	}

	/**
	 * Returns the policy at the given 1-based index.
	 *
	 * @param i the 1-based index
	 * @return the policy at that index
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	public Policy getByIndex(int i) {
		return collection.get(i - 1);
	}

	/**
	 * Returns a human-readable description of all policies, one per line,
	 * formatted as {@code <index> = <policy>}.
	 *
	 * @return the description string
	 */
	public String description() {
		return getPolicyData()
			.map(d -> d.index + " = " + d.policy.toString())
			.collect(Collectors.joining("\n")) + "\n";
	}
}
