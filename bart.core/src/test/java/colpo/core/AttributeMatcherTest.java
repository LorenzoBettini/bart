package colpo.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributeMatcherTest {

	private AttributeMatcher attributeMatcher;

	@BeforeEach
	void init() {
		attributeMatcher = new AttributeMatcher();
	}

	@Test
	void shouldMatchEmptyAttributes() {
		var attributes1 = new Attributes();
		var attributes2 = new Attributes().add("aName", "aValue")
			.add("aName1", "aValue1");
		assertTrue(attributeMatcher.match(attributes1, attributes2));
	}

	@Test
	void shouldMatchSubsetAttributes() {
		var attributes1 = new Attributes().add("aName", "aValue")
				.add("aName2", "aValue2");
		var attributes2 = new Attributes().add("aName", "aValue")
				.add("aName1", "aValue1")
				.add("aName2", "aValue2");
		var attributes3 = new Attributes().add("aName", "aValue")
				.add("aName2", "different value!");
		assertTrue(attributeMatcher.match(attributes1, attributes2));
		assertFalse(attributeMatcher.match(attributes2, attributes1));
		assertFalse(attributeMatcher.match(attributes3, attributes2));
	}

}
