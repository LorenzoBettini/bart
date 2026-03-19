package bart.core;

/**
 * A composite exchange where <em>either</em> the {@code left} or the
 * {@code right} sub-exchange must succeed.
 * <p>
 * The left exchange is attempted first; the right exchange is tried only if
 * the left one is denied.
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * // Either method of payment is acceptable
 * var exchange = new OrExchange(
 *     new SingleExchange(Participants.me(),
 *         new Attributes().add("payment/type", "cash"), Participants.requester()),
 *     new SingleExchange(Participants.me(),
 *         new Attributes().add("payment/type", "card"), Participants.requester()));
 * }
 * </p>
 *
 * @param left  the preferred exchange tried first
 * @param right the fallback exchange tried if {@code left} is denied
 * @author Lorenzo Bettini
 */
public record OrExchange(Exchange left, Exchange right) implements CompositeExchange {

	@Override
	public String toString() {
		return String.format("OR(%s, %s)", left, right);
	}
}
