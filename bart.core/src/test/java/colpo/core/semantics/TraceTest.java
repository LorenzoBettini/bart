package colpo.core.semantics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TraceTest {

	private Trace trace;

	@BeforeEach
	void init() {
		trace = new Trace();
	}

	@Test
	void testTrace() {
		trace.add("first line");
		trace.add("second line");
		assertEquals("""
				first line
				second line
				""", trace.toString());
	}

	@Test
	void testTraceIndent() {
		trace.add("first line");
		trace.addIndent();
		trace.add("second line");
		trace.addIndent();
		trace.add("third line");
		trace.removeIndent();
		trace.add("fourth line");
		trace.removeIndent();
		trace.add("fifth line");
		assertEquals("""
				first line
				  second line
				    third line
				  fourth line
				fifth line
				""", trace.toString());
	}

	@Test
	void testTraceAddAndThenIndent() {
		trace.addAndThenIndent("first line");
		trace.add("second line");
		trace.addAndThenIndent("third line");
		trace.add("fourth line");
		assertEquals("""
				first line
				  second line
				  third line
				    fourth line
				""", trace.toString());
	}

	@Test
	void testTraceAddInPreviousIndent() {
		trace.addAndThenIndent("first line");
		trace.add("second line");
		trace.addInPreviousIndent("third line");
		trace.add("fourth line");
		assertEquals("""
				first line
				  second line
				third line
				  fourth line
				""", trace.toString());
	}

	@Test
	void testTraceRemoveIndentAndThenAdd() {
		trace.addAndThenIndent("first line");
		trace.add("second line");
		trace.removeIndentAndThenAdd("third line");
		trace.add("fourth line");
		assertEquals("""
				first line
				  second line
				third line
				fourth line
				""", trace.toString());
	}

	@Test
	void testTraceReset() {
		trace.add("first line");
		trace.addIndent();
		trace.reset();
		trace.add("second line");
		assertEquals("second line\n", trace.toString());
	}
}
