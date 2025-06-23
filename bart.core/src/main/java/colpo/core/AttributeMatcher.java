/**
 *
 */
package colpo.core;

import java.util.Objects;

/**
 * @author Lorenzo Bettini
 */
public class AttributeMatcher {

	public boolean match(Attributes attributes1, Attributes attributes2) {
		if (attributes1.isEmpty()) {
			return true;
		}
		return attributes1.names().stream()
				.allMatch(n -> Objects.equals(attributes1.name(n), attributes2.name(n)));
	}

}
