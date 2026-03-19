/**
 *
 */
package bart.core;

/**
 * Represents a single, direct resource exchange between two participants.
 * <p>
 * This record describes that the party identified by {@code to} must provide
 * the specified {@code resource} on behalf of the party identified by
 * {@code from}. Both participants are resolved at evaluation time against the
 * current set of policies.
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * // Policy owner ("me") provides "paper" that must come from the requester
 * var exchange = new SingleExchange(
 *     Participants.me(),
 *     new Attributes().add("resource/type", "paper"),
 *     Participants.requester());
 * }
 * </p>
 *
 * @param to       the participant that must provide the resource
 * @param resource the attributes describing the required resource
 * @param from     the participant that this exchange is requested from
 * @author Lorenzo Bettini
 */
public record SingleExchange(ExchangeToParticipant to, Attributes resource, ExchangeFromParticipant from)
		implements Exchange {

	@Override
	public String toString() {
		return "Exchange[to=" + to + ", resource=" + resource + ", from=" + from + "]";
	}

}
