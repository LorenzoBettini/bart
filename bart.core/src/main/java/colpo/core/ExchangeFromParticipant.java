package colpo.core;

/**
 * Represents a participant specification usable in a "from" specification of an exchange
 *
 * @author Lorenzo Bettini
 */
public interface ExchangeFromParticipant extends Participant {

	default boolean isRequester() {
		return false;
	}
}
