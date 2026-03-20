package bart.core;

/**
 * Represents a participant specification usable in a "from" specification of an exchange.
 * <p>
 * Implementations include {@link RequesterParticipant} (the original requester)
 * and {@link QuantifiedParticipant} (matched by attributes).
 * </p>
 *
 * @see SingleExchange
 * @author Lorenzo Bettini
 */
public interface ExchangeFromParticipant extends Participant {

	/**
	 * Returns {@code true} if this participant represents the original requester
	 * of the request being evaluated.
	 *
	 * @return {@code true} if this is the requester participant
	 */
	default boolean isRequester() {
		return false;
	}
}
