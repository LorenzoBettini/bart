/**
 *
 */
package bart.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Party indexes are 1-based, so the first party is 1, the second is 2, and so on.
 * 
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
