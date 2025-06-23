/**
 *
 */
package colpo.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Lorenzo Bettini
 */
public class ContextHandler {

	private Map<Integer, Attributes> context = new LinkedHashMap<>();

	public Attributes ofParty(int partyIndex) {
		return context.computeIfAbsent(partyIndex,
				key -> new Attributes());
	}

	public ContextHandler add(int partyIndex, String attributeName, Object attributeValue) {
		ofParty(partyIndex)
			.add(attributeName, attributeValue);
		return this;
	}

}
