package bart.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContextHandlerTest {

	private ContextHandler contextHandler;

	@BeforeEach
	void init() {
		contextHandler = new ContextHandler();
	}

	@Test
	void testFluentApi() {
		var result = contextHandler
			.add(1, "anAttribute", "aValue")
			.add(2, "anotherAttribute", () -> "anotherValue");
		assertThat(result)
			.isNotNull()
			.isSameAs(contextHandler);
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

	@Test
	void testAddSupplier() {
		var counter = new AtomicInteger(0);
		contextHandler.add(1, "dynamic", counter::incrementAndGet);
		var attributes = contextHandler.ofParty(1);
		// value is re-computed on each call
		assertEquals(1, attributes.name("dynamic"));
		assertEquals(2, attributes.name("dynamic"));
	}
}
