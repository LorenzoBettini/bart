/**
 *
 */
package bart.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A key-value container that describes resources, participants, or conditions.
 * <p>
 * Keys are unique attribute names (e.g. {@code "resource/type"}, {@code "role"})
 * and values can be any {@link Object}. Order of insertion is preserved.
 * Duplicate keys are rejected at add-time.
 * </p>
 *
 * <p>Example usage:
 * {@snippet :
 * var attrs = new Attributes()
 *     .add("resource/type", "printer")
 *     .add("role", "Provider");
 * Object type = attrs.name("resource/type"); // "printer"
 * }
 * </p>
 *
 * @author Lorenzo Bettini
 */
public class Attributes {

	private Map<String, Object> attributeMap = new LinkedHashMap<>();

	/**
	 * Adds a new attribute with the given name and value.
	 *
	 * @param attributeName the unique attribute name; must not already be present
	 * @param attributeValue the attribute value
	 * @return {@code this} to allow fluent chaining
	 * @throws IllegalArgumentException if {@code attributeName} is already present
	 */
	public Attributes add(String attributeName, Object attributeValue) {
		Object previous = attributeMap.put(attributeName, attributeValue);
		if (previous != null) {
			throw new IllegalArgumentException(
				String.format("'%s' is already present as '%s'", attributeName, previous));
		}
		return this;
	}

	/**
	 * Returns the value associated with the given attribute name, or {@code null}
	 * if no such attribute exists.
	 *
	 * @param attributeName the attribute name to look up
	 * @return the attribute value, or {@code null} if not present
	 */
	public Object name(String attributeName) {
		return attributeMap.get(attributeName);
	}

	@Override
	public String toString() {
		return "[" +
			attributeMap.entrySet().stream()
			.map(e -> String.format("(%s : %s)", e.getKey(), e.getValue()))
			.collect(Collectors.joining(", ")) +
			"]";
	}

	/**
	 * Returns {@code true} if this container holds no attributes.
	 *
	 * @return {@code true} if empty
	 */
	public boolean isEmpty() {
		return attributeMap.isEmpty();
	}

	/**
	 * Returns the set of attribute names in insertion order.
	 *
	 * @return an unmodifiable view of the attribute names
	 */
	public Collection<String> names() {
		return attributeMap.keySet();
	}

	@Override
	public int hashCode() {
		return Objects.hash(attributeMap);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		Attributes other = (Attributes) obj;
		return Objects.equals(attributeMap, other.attributeMap);
	}
}
