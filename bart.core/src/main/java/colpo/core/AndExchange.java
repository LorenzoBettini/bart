package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public record AndExchange(Exchange left, Exchange right) implements CompositeExchange {

	@Override
	public String toString() {
		return String.format("AND(%s, %s)", left, right);
	}
}
