package colpo.core;

/**
 * Represents a participant specification usable in a "to" specification of an exchange
 *
 * @author Lorenzo Bettini
 */
public interface ExchangeToParticipant extends Participant {

	default boolean isMe() {
		return false;
	}

}
