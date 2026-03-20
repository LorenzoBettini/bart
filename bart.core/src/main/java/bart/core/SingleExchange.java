/**
 *
 */
package bart.core;

/**
 * Represents a single, direct resource exchange between two participants.
 * <p>
 * This record describes that the party identified by {@code from} must provide
 * the specified {@code resource} to the party identified by {@code to}. Both
 * participants are resolved at evaluation time against the current set of
 * policies.
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * // The requester must provide "paper" to the policy owner ("me")
 * var exchange = new SingleExchange(
 *     Participants.me(),
 *     new Attributes().add("resource/type", "paper"),
 *     Participants.requester());
 * }
 * </p>
 *
 * @param to       the participant that must receive the resource
 * @param resource the attributes describing the required resource
 * @param from     the participant that must provide the resource
 * @author Lorenzo Bettini
 */
public record SingleExchange(ExchangeToParticipant to, Attributes resource, ExchangeFromParticipant from)
		implements Exchange {

	@Override
	public String toString() {
		return "Exchange[to=" + to + ", resource=" + resource + ", from=" + from + "]";
	}

}
