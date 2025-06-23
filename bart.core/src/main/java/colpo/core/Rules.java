package colpo.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Lorenzo Bettini
 */
public class Rules {

	public static record RuleData(int index, Rule rule) {

	}

	private List<Rule> collection = new ArrayList<>();

	public Rules add(Rule rule) {
		collection.add(rule);
		return this;
	}

	public Stream<RuleData> getRuleData() {
		return IntStream.range(0, collection.size())
			.mapToObj(i -> new RuleData(i + 1, collection.get(i)));
	}

	@Override
	public String toString() {
		return collection.toString();
	}
}
