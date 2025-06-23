package colpo.core;

/**
 * @author Lorenzo Bettini
 */
@FunctionalInterface
public interface ExpressionCode {
	boolean evaluate(AttributesResolver context) throws Exception; // NOSONAR
}
