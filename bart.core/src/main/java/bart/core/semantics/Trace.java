package bart.core.semantics;

/**
 * Accumulates a human-readable, indented log of the step-by-step decisions
 * made during semantic evaluation.
 * <p>
 * Indentation is increased when entering a new evaluation scope and decreased
 * when leaving it, producing a tree-like view of the evaluation process.
 * The trace is reset at the start of each top-level
 * {@link Semantics#evaluate(bart.core.Request)} call.
 * </p>
 *
 * @author Lorenzo Bettini
 */
public class Trace {

	private StringBuilder builder = new StringBuilder();
	private int indent = 0;

	/**
	 * Appends a line to the trace at the current indentation level.
	 *
	 * @param string the text to append
	 */
	public void add(String string) {
		builder.append(String.format("%s%s%s",
			" ".repeat(indent), string, "\n"));
	}

	@Override
	public String toString() {
		return builder.toString();
	}

	/**
	 * Increases the indentation level by two spaces.
	 */
	public void addIndent() {
		indent += 2;
	}

	/**
	 * Decreases the indentation level by two spaces.
	 */
	public void removeIndent() {
		indent -= 2;
	}

	/**
	 * Resets the trace to its initial empty state and clears the indentation level.
	 */
	public void reset() {
		builder = new StringBuilder();
		indent = 0;
	}

	/**
	 * Appends a line at the current indentation level and then increases
	 * the indentation for subsequent entries.
	 *
	 * @param string the text to append
	 */
	public void addAndThenIndent(String string) {
		add(string);
		addIndent();
	}

	/**
	 * Temporarily decreases the indentation level, appends a line, then
	 * restores the previous (higher) indentation.
	 * <p>
	 * This is used for "connector" labels such as AND/OR that logically sit
	 * between two indented blocks.
	 * </p>
	 *
	 * @param string the text to append at the parent indentation level
	 */
	public void addInPreviousIndent(String string) {
		removeIndent();
		add(string);
		addIndent();
	}

	/**
	 * Decreases the indentation level and then appends a line (used for closing
	 * scope summary lines).
	 *
	 * @param string the text to append
	 */
	public void removeIndentAndThenAdd(String string) {
		removeIndent();
		add(string);
	}
}
