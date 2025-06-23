/**
 *
 */
package colpo.core;

/**
 * @author Lorenzo Bettini
 */
public record Request(IndexParticipant requester, Attributes resource, RequestFromParticipant from) {

	public Request {
		if (requester != null && requester.getIndex() > 0 && requester.getIndex() == from.getIndex()) {
			throw new IllegalArgumentException("requester and from are the same: " + requester.getIndex());
		}
	}

	/**
	 * Create a copy of this request but replacing the "from" Participant
	 * with the Participant index.
	 *
	 * @param participantIndex
	 * @return
	 */
	public Request withFrom(int participantIndex) {
		return new Request(requester, resource, Participants.index(participantIndex));
	}
}
