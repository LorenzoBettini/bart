package bart.core;

/**
 * A participant identified by a concrete 1-based index in the policies list.
 * <p>
 * Use the factory method {@link Participants#index(int)} to create instances.
 * {@code IndexParticipant} can act as both the requester of a request and as
 * the target ("to") of an exchange.
 * </p>
 *
 * @param index the 1-based index of this participant in the policies list
 * @author Lorenzo Bettini
 */
public record IndexParticipant(int index) implements RequestFromParticipant, ExchangeToParticipant {

	/**
	 * Returns the 1-based index of this participant.
	 *
	 * @return the participant index
	 */
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String toString() {
		return "" + index;
	}
}
