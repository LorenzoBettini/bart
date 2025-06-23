package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public record OrExchange(Exchange left, Exchange right) implements CompositeExchange {

	@Override
	public String toString() {
		return String.format("OR(%s, %s)", left, right);
	}
}
