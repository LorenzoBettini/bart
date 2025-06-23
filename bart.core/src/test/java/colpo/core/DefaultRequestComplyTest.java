package colpo.core;

import static colpo.core.Participants.index;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultRequestComplyTest {

	private DefaultRequestComply comply;

	@BeforeEach
	void setup() {
		comply = new DefaultRequestComply(new AttributeMatcher());
	}

	@Test
	void testComply() {
		var r1 = new Request(
			index(1),
			new Attributes(),
			index(2)
		);
		var r2 = new Request(
			index(1),
			new Attributes(),
			index(2)
		);
		var r3 = new Request(
			index(3),
			new Attributes(),
			index(2)
		);
		var r4 = new Request(
			index(3),
			new Attributes(),
			index(1)
		);
		var r5 = new Request(
			index(3),
			new Attributes().add("name", "value"),
			index(1)
		);
		assertTrue(comply.test(r1, r2));
		// different requester
		assertFalse(comply.test(r3, r2));
		// same requester, different from
		assertFalse(comply.test(r4, r3));
		// same requester, same from, non matching resource
		assertFalse(comply.test(r5, r4));
	}

}
