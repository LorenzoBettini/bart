package bart.core;

/**
 * Represents a participant specification usable in a "to" specification of an exchange.
 * <p>
 * Implementations include {@link MeParticipant} (the policy owner) and
 * {@link QuantifiedParticipant} / {@link IndexParticipant} (specific or matched parties).
 * </p>
 *
 * @see SingleExchange
 * @author Lorenzo Bettini
 */
public interface ExchangeToParticipant extends Participant {

	/**
	 * Returns {@code true} if this participant represents the policy owner ("me").
	 *
	 * @return {@code true} if this is the "me" participant
	 */
	default boolean isMe() {
		return false;
	}

}
