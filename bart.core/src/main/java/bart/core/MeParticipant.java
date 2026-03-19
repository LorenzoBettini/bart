package bart.core;

/**
 * Marker interface for the "me" participant, representing the policy owner
 * (i.e. the party whose policy is currently being evaluated).
 * <p>
 * Use the factory method {@link Participants#me()} to obtain the shared singleton instance.
 * A {@code MeParticipant} can appear only as the target ("to") of a {@link SingleExchange}.
 * </p>
 *
 * @see Participants#me()
 */
public interface MeParticipant extends ExchangeToParticipant {

	/**
	 * Always returns {@code true}, indicating that this participant refers to
	 * the policy owner.
	 *
	 * @return {@code true}
	 */
	@Override
	default boolean isMe() {
		return true;
	}

}
