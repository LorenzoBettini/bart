/**
 *
 */
package colpo.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Lorenzo Bettini
 */
public class Attributes {

	private Map<String, Object> attributeMap = new LinkedHashMap<>();

	public Attributes add(String attributeName, Object attributeValue) {
		Object previous = attributeMap.put(attributeName, attributeValue);
		if (previous != null) {
			throw new IllegalArgumentException(
				String.format("'%s' is already present as '%s'", attributeName, previous));
		}
		return this;
	}

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

	public boolean isEmpty() {
		return attributeMap.isEmpty();
	}

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
