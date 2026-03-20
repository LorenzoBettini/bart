package bart.core;

/**
 * A functional interface representing a boolean condition evaluated during policy rule
 * assessment.
 * <p>
 * Implementations receive a {@link NameResolver} that provides access to the
 * attributes of the current request, context, and parties, and must return
 * {@code true} if the condition holds. Checked exceptions are permitted to
 * propagate (the semantics engine treats them as a denied condition).
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * ExpressionCode timeCondition = ctx ->
 *     ctx.name("time", Integer.class) < 18;
 * }
 * </p>
 *
 * @see ExpressionWithDescription
 * @see NameResolver
 * @author Lorenzo Bettini
 */
@FunctionalInterface
public interface ExpressionCode {
	/**
	 * Evaluates this condition.
	 *
	 * @param nameResolver provides attribute values for the current evaluation context
	 * @return {@code true} if the condition is satisfied
	 * @throws Exception if an error occurs during evaluation (treated as denial)
	 */
	boolean evaluate(NameResolver nameResolver) throws Exception; // NOSONAR
}
