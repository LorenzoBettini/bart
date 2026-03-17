package bart.core;

public class ExpressionWithDescription implements ExpressionCode {

	private ExpressionCode expressionCode;
	private String description;

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
