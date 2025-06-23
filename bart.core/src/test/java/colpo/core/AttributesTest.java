package colpo.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributesTest {

	private Attributes attributes;

	@BeforeEach
	void init() {
		attributes = new Attributes();
	}

	@Test
	void shouldAddWhenNotPresent() {
		attributes.add("aName", "aValue")
			.add("aName1", "aValue1");
		assertEquals("aValue", attributes.name("aName"));
		assertEquals("aValue1", attributes.name("aName1"));
		assertNull(attributes.name("non existent"));
	}

	@Test
	void shouldThrowWhenPresent() {
		attributes.add("aName", "aValue")
			.add("aName1", "aValue1");
		assertThatThrownBy(() -> attributes.add("aName", "anotherValue"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("'aName' is already present as 'aValue'");
	}

	@Test
	void attributesToString() {
		attributes.add("aName", "aValue")
			.add("aName1", "aValue1");
		assertEquals("[(aName : aValue), (aName1 : aValue1)]", attributes.toString());
	}

	@Test
	void attributesNames() {
		attributes.add("aName", "aValue")
			.add("aName1", "aValue1");
		assertThat(attributes.names())
			.containsExactlyInAnyOrder("aName", "aName1");
	}
}
