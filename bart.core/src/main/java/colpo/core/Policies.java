package colpo.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Lorenzo Bettini
 *
 * Our Policies are indexed starting from 1, not from 0.
 */
public class Policies {

	public static record PolicyData(int index, Policy policy) {

	}

	private List<Policy> collection = new ArrayList<>();

	public Policies add(Policy policy) {
		collection.add(policy);
		return this;
	}

	public Stream<PolicyData> getPolicyData() {
		return IntStream.range(0, collection.size())
			.mapToObj(i -> new PolicyData(i + 1, collection.get(i)));
	}

	public Policy getByIndex(int i) {
		return collection.get(i - 1);
	}

	public String description() {
		return getPolicyData()
			.map(d -> d.index + " = " + d.policy.toString())
			.collect(Collectors.joining("\n")) + "\n";
	}
}
