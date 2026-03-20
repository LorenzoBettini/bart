package bart.core;

/**
 * A sealed interface representing a composite exchange made up of two
 * sub-exchanges.
 * <p>
 * Permitted implementations:
 * <ul>
 *   <li>{@link AndExchange} — both sub-exchanges must succeed</li>
 *   <li>{@link OrExchange} — at least one sub-exchange must succeed</li>
 * </ul>
 * </p>
 *
 * @author Lorenzo Bettini
 */
public sealed interface CompositeExchange extends Exchange permits AndExchange, OrExchange {

	/**
	 * Returns the left (first) sub-exchange.
	 *
	 * @return the left sub-exchange
	 */
	Exchange left();

	/**
	 * Returns the right (second) sub-exchange.
	 *
	 * @return the right sub-exchange
	 */
	Exchange right();
}