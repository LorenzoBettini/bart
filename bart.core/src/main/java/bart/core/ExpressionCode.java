package bart.core;

/**
 * @author Lorenzo Bettini
 */
@FunctionalInterface
public interface ExpressionCode {
	boolean evaluate(NameResolver nameResolver) throws Exception; // NOSONAR
}
