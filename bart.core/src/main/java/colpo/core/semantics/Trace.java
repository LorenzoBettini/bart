package colpo.core.semantics;

/**
 * @author Lorenzo Bettini
 */
public class Trace {

	private StringBuilder builder = new StringBuilder();
	private int indent = 0;

	public void add(String string) {
		builder.append(String.format("%s%s%s",
			" ".repeat(indent), string, "\n"));
	}

	@Override
	public String toString() {
		return builder.toString();
	}

	public void addIndent() {
		indent += 2;
	}

	public void removeIndent() {
		indent -= 2;
	}

	public void reset() {
		builder = new StringBuilder();
		indent = 0;
	}

	public void addAndThenIndent(String string) {
		add(string);
		addIndent();
	}

	public void addInPreviousIndent(String string) {
		removeIndent();
		add(string);
		addIndent();
	}

	public void removeIndentAndThenAdd(String string) {
		removeIndent();
		add(string);
	}
}
