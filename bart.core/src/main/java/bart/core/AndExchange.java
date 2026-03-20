package bart.core;

/**
 * A composite exchange that requires <em>both</em> the {@code left} and the
 * {@code right} sub-exchanges to succeed.
 * <p>
 * If the left exchange is denied, the right exchange is not attempted.
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * // Both exchanges must succeed: requester provides "passport" AND provides "payment"
 * var exchange = new AndExchange(
 *     new SingleExchange(Participants.me(),
 *         new Attributes().add("document/type", "passport"), Participants.requester()),
 *     new SingleExchange(Participants.me(),
 *         new Attributes().add("resource/type", "payment"), Participants.requester()));
 * }
 * </p>
 *
 * @param left  the first exchange that must succeed
 * @param right the second exchange that must also succeed
 * @author Lorenzo Bettini
 */
public record AndExchange(Exchange left, Exchange right) implements CompositeExchange {

	@Override
	public String toString() {
		return String.format("AND(%s, %s)", left, right);
	}
}
