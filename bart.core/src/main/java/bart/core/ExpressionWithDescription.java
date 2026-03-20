package bart.core;

/**
 * An {@link ExpressionCode} decorator that pairs an expression with a
 * human-readable description used in trace output and {@link #toString()}.
 * <p>
 * Use this class instead of a plain lambda when you want the trace to show
 * a meaningful label for the condition rather than an anonymous lambda reference.
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * var condition = new ExpressionWithDescription(
 *     ctx -> ctx.name("time", Integer.class) < 18,
 *     "time < 18");
 * }
 * </p>
 *
 * @author Lorenzo Bettini
 */
public class ExpressionWithDescription implements ExpressionCode {

	private ExpressionCode expressionCode;
	private String description;

	/**
	 * Creates a new decorated expression.
	 *
	 * @param expressionCode the expression to delegate to
	 * @param description    a human-readable label for trace/debugging output
	 */
	public ExpressionWithDescription(ExpressionCode expressionCode, String description) {
		this.expressionCode = expressionCode;
		this.description = description;
	}

	@Override
	public boolean evaluate(NameResolver nameResolver) throws Exception {
		return expressionCode.evaluate(nameResolver);
	}

	@Override
	public String toString() {
		return description;
	}
}
