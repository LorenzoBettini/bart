package bart.core;

/**
 * Marker interface for the "requester" participant, representing the party that
 * initiated the original resource request.
 * <p>
 * Use the factory method {@link Participants#requester()} to obtain the shared
 * singleton instance. A {@code RequesterParticipant} can appear only as the
 * source ("from") of a {@link SingleExchange}.
 * </p>
 *
 * @see Participants#requester()
 */
public interface RequesterParticipant extends ExchangeFromParticipant {

	/**
	 * Always returns {@code true}, indicating that this participant refers to
	 * the original requester.
	 *
	 * @return {@code true}
	 */
	@Override
	default boolean isRequester() {
		return true;
	}
}
