package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public class Rule {

	private static final ExpressionWithDescription TRUE =
			new ExpressionWithDescription(context -> true, "true");

	private static final Attributes EMPTY_ATTRIBUTES = new Attributes();

	private final Attributes resource;
	private final ExpressionCode condition;
	private final Exchange exchange;

	public Rule() {
		this(EMPTY_ATTRIBUTES, TRUE, null);
	}

	public Rule(Attributes resource) {
		this(resource, TRUE, null);
	}

	public Rule(Attributes resource, ExpressionCode condition) {
		this(resource, condition, null);
	}

	public Rule(Attributes resource, Exchange exchange) {
		this(resource, TRUE, exchange);
	}

	public Rule(Attributes resource, ExpressionCode condition, Exchange exchange) {
		this.resource = resource;
		this.condition = condition;
		this.exchange = exchange;
	}

	public Attributes getResource() {
		return resource;
	}

	public ExpressionCode getCondition() {
		return condition;
	}

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
