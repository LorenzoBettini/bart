package colpo.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContextHandlerTest {

	private ContextHandler contextHandler;

	@BeforeEach
	void init() {
		contextHandler = new ContextHandler();
	}

	@Test
	void testWhenEmpty() {
		var attributes = contextHandler.ofParty(1);
		assertThat(attributes)
			.isNotNull();
	}

	@Test
	void testAddAttribute() {
		contextHandler
			.add(1, "anAttribute1", "aValue1")
			.add(1, "anAttribute2", "aValue2");
		var attributes = contextHandler.ofParty(1);
		assertEquals("aValue2", attributes.name("anAttribute2"));
		assertEquals("aValue1", attributes.name("anAttribute1"));
	}
}
