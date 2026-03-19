package bart.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * An ordered, 1-based indexed collection of {@link Rule} objects belonging to
 * a single {@link Policy}.
 * <p>
 * During semantic evaluation rules are tested in order; the first matching
 * rule wins.
 * </p>
 *
 * @author Lorenzo Bettini
 */
public class Rules {

	/**
	 * Associates a 1-based index with its {@link Rule}.
	 *
	 * @param index the 1-based position of the rule within the policy
	 * @param rule  the rule at that position
	 */
	public static record RuleData(int index, Rule rule) {

	}

	private List<Rule> collection = new ArrayList<>();

	/**
	 * Appends a rule to the collection.
	 *
	 * @param rule the rule to add
	 * @return {@code this} to allow fluent chaining
	 */
	public Rules add(Rule rule) {
		collection.add(rule);
		return this;
	}

	/**
	 * Returns a stream of {@link RuleData} records pairing each rule with its
	 * 1-based index.
	 *
	 * @return a sequential stream of rule data
	 */
	public Stream<RuleData> getRuleData() {
		return IntStream.range(0, collection.size())
			.mapToObj(i -> new RuleData(i + 1, collection.get(i)));
	}

	@Override
	public String toString() {
		return collection.toString();
	}
}
