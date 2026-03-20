/**
 *
 */
package bart.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Stores dynamic, per-party contextual attributes that supplement policy attributes
 * during semantic evaluation.
 * <p>
 * Party indexes are 1-based: the first party is {@code 1}, the second is {@code 2},
 * and so on. Attribute values may be plain objects or lazy {@link java.util.function.Supplier
 * Supplier} instances; in the latter case the supplier is called each time the
 * attribute is read (useful for time-sensitive or computed values).
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * var ctx = new ContextHandler()
 *     .add(1, "location", "warehouse")
 *     .add(2, "time", () -> LocalTime.now());
 * }
 * </p>
 *
 * @author Lorenzo Bettini
 */
public class ContextHandler {

	private Map<Integer, Attributes> context = new LinkedHashMap<>();

	/**
	 * Returns the {@link Attributes} associated with the given party index,
	 * creating an empty entry if none exists yet.
	 *
	 * @param partyIndex the 1-based index of the party
	 * @return the attributes for the party (never {@code null})
	 */
	public Attributes ofParty(int partyIndex) {
		return context.computeIfAbsent(partyIndex,
				key -> new DynamicAttributes());
	}

	/**
	 * Adds a static attribute for the given party.
	 *
	 * @param partyIndex the 1-based index of the party
	 * @param attributeName the attribute name
	 * @param attributeValue the attribute value
	 * @return {@code this} to allow fluent chaining
	 */
	public ContextHandler add(int partyIndex, String attributeName, Object attributeValue) {
		ofParty(partyIndex)
			.add(attributeName, attributeValue);
		return this;
	}

	/**
	 * Adds a lazy (supplier-based) attribute for the given party.
	 * The supplier is called each time the attribute is read.
	 *
	 * @param partyIndex the 1-based index of the party
	 * @param attributeName the attribute name
	 * @param attributeValue a supplier whose result is returned on each read
	 * @return {@code this} to allow fluent chaining
	 */
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
