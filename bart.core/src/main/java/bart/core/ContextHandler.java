/**
 *
 */
package bart.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Party indexes are 1-based, so the first party is 1, the second is 2, and so on.
 * 
 * @author Lorenzo Bettini
 */
public class ContextHandler {

	private Map<Integer, Attributes> context = new LinkedHashMap<>();

	public Attributes ofParty(int partyIndex) {
		return context.computeIfAbsent(partyIndex,
				key -> new DynamicAttributes());
	}

	public ContextHandler add(int partyIndex, String attributeName, Object attributeValue) {
		ofParty(partyIndex)
			.add(attributeName, attributeValue);
		return this;
	}

	public ContextHandler add(int partyIndex, String attributeName, Supplier<?> attributeValue) {
		ofParty(partyIndex)
			.add(attributeName, attributeValue);
		return this;
	}

	private static class DynamicAttributes extends Attributes {
		@Override
		public Object name(String attributeName) {
			var value = super.name(attributeName);
			if (value instanceof Supplier<?> supplier) {
				return supplier.get();
			}
			return value;
		}
	}

}
