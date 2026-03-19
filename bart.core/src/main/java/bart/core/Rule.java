package bart.core;

/**
 * Defines a single rule within a {@link Policy}.
 * <p>
 * A rule specifies:
 * <ul>
 *   <li>a resource pattern (required {@link Attributes}) that an incoming request must match</li>
 *   <li>an optional {@link ExpressionCode} condition that must hold at evaluation time</li>
 *   <li>an optional {@link Exchange} that the policy owner requires in return</li>
 * </ul>
 * When the resource matches and the condition is satisfied, the exchange (if any)
 * is evaluated recursively. If there is no exchange, access is granted unconditionally
 * once the condition holds.
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * // Grant access to "printer" unconditionally
 * var rule1 = new Rule(new Attributes().add("resource/type", "printer"));
 *
 * // Grant access only when time is within office hours, requiring a paper in return
 * var rule2 = new Rule(
 *     new Attributes().add("resource/type", "printer"),
 *     new ExpressionWithDescription(
 *         ctx -> ctx.name("time", Integer.class) < 18, "time < 18"),
 *     new SingleExchange(Participants.me(),
 *         new Attributes().add("resource/type", "paper"),
 *         Participants.requester()));
 * }
 * </p>
 *
 * @author Lorenzo Bettini
 */
public class Rule {

	private static final ExpressionWithDescription TRUE =
			new ExpressionWithDescription(context -> true, "true");

	private static final Attributes EMPTY_ATTRIBUTES = new Attributes();

	private final Attributes resource;
	private final ExpressionCode condition;
	private final Exchange exchange;

	/**
	 * Creates a rule that matches any resource, has a {@code true} condition,
	 * and requires no exchange.
	 */
	public Rule() {
		this(EMPTY_ATTRIBUTES, TRUE, null);
	}

	/**
	 * Creates a rule that matches the given resource pattern, has a {@code true}
	 * condition, and requires no exchange.
	 *
	 * @param resource the attributes that the requested resource must match
	 */
	public Rule(Attributes resource) {
		this(resource, TRUE, null);
	}

	/**
	 * Creates a rule that matches the given resource pattern and requires the
	 * given condition to hold, but requires no exchange.
	 *
	 * @param resource  the attributes that the requested resource must match
	 * @param condition the condition that must be {@code true} for access to be granted
	 */
	public Rule(Attributes resource, ExpressionCode condition) {
		this(resource, condition, null);
	}

	/**
	 * Creates a rule that matches the given resource pattern with a {@code true}
	 * condition and requires the specified exchange.
	 *
	 * @param resource the attributes that the requested resource must match
	 * @param exchange the exchange the policy owner requires in return
	 */
	public Rule(Attributes resource, Exchange exchange) {
		this(resource, TRUE, exchange);
	}

	/**
	 * Creates a fully-specified rule.
	 *
	 * @param resource  the attributes that the requested resource must match
	 * @param condition the condition that must hold
	 * @param exchange  the exchange required in return, or {@code null} if none
	 */
	public Rule(Attributes resource, ExpressionCode condition, Exchange exchange) {
		this.resource = resource;
		this.condition = condition;
		this.exchange = exchange;
	}

	/**
	 * Returns the resource attributes that an incoming request must match for
	 * this rule to apply.
	 *
	 * @return the resource pattern; never {@code null}
	 */
	public Attributes getResource() {
		return resource;
	}

	/**
	 * Returns the condition that must evaluate to {@code true} for this rule to
	 * grant access.
	 *
	 * @return the condition expression; never {@code null} (defaults to {@code true})
	 */
	public ExpressionCode getCondition() {
		return condition;
	}

	/**
	 * Returns the exchange the policy owner requires in return, or {@code null}
	 * if access is granted without any exchange.
	 *
	 * @return the exchange, or {@code null}
	 */
	public Exchange getExchange() {
		return exchange;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("resource=");
		stringBuilder.append(resource.toString());
		stringBuilder.append(", condition=");
		stringBuilder.append(condition.toString());
		stringBuilder.append((exchange != null ? ", exchange=" + exchange.toString() : ""));
		return stringBuilder.toString();
	}
}
