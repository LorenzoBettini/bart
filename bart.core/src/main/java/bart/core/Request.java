/**
 *
 */
package bart.core;

/**
 * Represents a resource request from one participant to another.
 * <p>
 * A request identifies:
 * <ul>
 *   <li>the {@code requester} — the party asking for the resource (by 1-based index)</li>
 *   <li>the {@code resource} — the attributes describing the requested resource</li>
 *   <li>the {@code from}     — who the resource should come from (may be a concrete index,
 *       or a quantified/symbolic participant that is resolved at evaluation time)</li>
 * </ul>
 * The requester and the {@code from} participant must be different parties.
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * // Party 1 asks any party with role "Provider" for a "printer"
 * var request = new Request(
 *     Participants.index(1),
 *     new Attributes().add("resource/type", "printer"),
 *     Participants.any(new Attributes().add("role", "Provider")));
 * }
 * </p>
 *
 * @param requester the party initiating the request (1-based index)
 * @param resource  the attributes describing the requested resource
 * @param from      the participant the resource is requested from
 * @author Lorenzo Bettini
 */
public record Request(IndexParticipant requester, Attributes resource, RequestFromParticipant from) {

	public Request {
		if (requester != null && requester.getIndex() > 0 && requester.getIndex() == from.getIndex()) {
			throw new IllegalArgumentException("requester and from are the same: " + requester.getIndex());
		}
	}

	/**
	 * Creates a copy of this request replacing the {@code from} participant with
	 * an {@link IndexParticipant} for the given index.
	 * <p>
	 * This is used internally by the semantics engine when a quantified
	 * {@code from} participant has been resolved to a concrete party.
	 * </p>
	 *
	 * @param participantIndex the 1-based index of the concrete party
	 * @return a new request with the same requester and resource, but a concrete {@code from}
	 */
	public Request withFrom(int participantIndex) {
		return new Request(requester, resource, Participants.index(participantIndex));
	}
}
