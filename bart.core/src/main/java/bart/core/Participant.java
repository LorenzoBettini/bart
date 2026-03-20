package bart.core;

/**
 * Base interface for all participant types in the Bart framework.
 * <p>
 * A participant represents an entity (identified by an index in a policy list,
 * a quantifier over a set of entities, or a symbolic role such as "me" or
 * "requester") that can take part in a resource exchange or request.
 * </p>
 *
 * @see IndexParticipant
 * @see QuantifiedParticipant
 * @see MeParticipant
 * @see RequesterParticipant
 */
public interface Participant {

	static final Attributes EMPTY_ATTRIBUTES = new Attributes();

	/**
	 * Returns the 1-based index of this participant in the policies list,
	 * or {@code -1} if this participant is not identified by a fixed index
	 * (e.g. quantified, me, or requester participants).
	 *
	 * @return the participant index, or {@code -1}
	 */
	default int getIndex() {
		return -1;
	}

	/**
	 * Returns {@code true} if this participant represents <em>all</em> matching
	 * parties (i.e. {@link QuantifiedParticipant} with {@code ALL} quantifier).
	 *
	 * @return {@code true} if this is an "all" quantified participant
	 */
	default boolean isAll() {
		return false;
	}

	/**
	 * Returns the matching attributes used to identify the target parties when
	 * this participant is not addressed by a fixed index.
	 *
	 * @return the matching attributes; never {@code null}
	 */
	default Attributes getAttributes() {
		return EMPTY_ATTRIBUTES;
	}
}
