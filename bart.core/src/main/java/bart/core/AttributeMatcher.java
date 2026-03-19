/**
 *
 */
package bart.core;

import java.util.Objects;

/**
 * Checks whether one set of {@link Attributes} is a subset match of another.
 * <p>
 * An empty first set of attributes matches everything (wildcard semantics).
 * Otherwise, every attribute name present in the first set must appear in the
 * second set with an equal value.
 * </p>
 *
 * @author Lorenzo Bettini
 */
public class AttributeMatcher {

	/**
	 * Returns {@code true} if every attribute present in {@code attributes1} also
	 * appears in {@code attributes2} with the same value.
	 * <p>
	 * An empty {@code attributes1} always matches (wildcard).
	 * </p>
	 *
	 * @param attributes1 the required attributes (may be empty)
	 * @param attributes2 the attributes to match against
	 * @return {@code true} if {@code attributes1} is a subset of {@code attributes2}
	 */
	public boolean match(Attributes attributes1, Attributes attributes2) {
		if (attributes1.isEmpty()) {
			return true;
		}
		return attributes1.names().stream()
				.allMatch(n -> Objects.equals(attributes1.name(n), attributes2.name(n)));
	}

}
