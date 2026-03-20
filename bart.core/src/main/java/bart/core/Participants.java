package bart.core;

import bart.core.QuantifiedParticipant.Quantifier;

/**
 * Factory class providing static convenience methods for creating all participant types.
 * <p>
 * This is the primary entry-point for constructing participants used in
 * {@link Request} and {@link Exchange} definitions.
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * import static bart.core.Participants.*;
 *
 * var request = new Request(
 *     index(1),
 *     new Attributes().add("resource/type", "printer"),
 *     any(new Attributes().add("role", "Provider")));
 *
 * var exchange = new SingleExchange(
 *     me(),
 *     new Attributes().add("resource/type", "paper"),
 *     requester());
 * }
 * </p>
 *
 * @author Lorenzo Bettini
 */
public class Participants {

	private Participants() {
		// Only static methods
	}

	private static final MeParticipant ME = new MeParticipant() {
		@Override
		public String toString() {
			return "ME";
		}
	};

	private static final RequesterParticipant REQUESTER = new RequesterParticipant() {
		@Override
		public String toString() {
			return "REQUESTER";
		}
	};

	/**
	 * Returns an {@link IndexParticipant} for the given 1-based party index.
	 *
	 * @param index the 1-based index of the party in the policies list
	 * @return an {@code IndexParticipant} for {@code index}
	 */
	public static IndexParticipant index(int index) {
		return new IndexParticipant(index);
	}

	/**
	 * Returns the shared {@link MeParticipant} singleton that refers to the
	 * policy owner in an exchange definition.
	 *
	 * @return the "me" participant
	 */
	public static MeParticipant me() {
		return ME;
	}

	/**
	 * Returns the shared {@link RequesterParticipant} singleton that refers to
	 * the original requester in an exchange definition.
	 *
	 * @return the "requester" participant
	 */
	public static RequesterParticipant requester() {
		return REQUESTER;
	}

	/**
	 * Returns a {@link QuantifiedParticipant} that matches <em>any</em> party
	 * whose attributes satisfy the given {@code attributes}.
	 *
	 * @param attributes the attributes used to match target parties
	 * @return a new {@code ANY} quantified participant
	 */
	public static QuantifiedParticipant any(Attributes attributes) {
		return new QuantifiedParticipant(Quantifier.ANY, attributes);
	}

	/**
	 * Returns a {@link QuantifiedParticipant} that matches <em>all</em> parties
	 * whose attributes satisfy the given {@code attributes}.
	 *
	 * @param attributes the attributes used to match target parties
	 * @return a new {@code ALL} quantified participant
	 */
	public static QuantifiedParticipant all(Attributes attributes) {
		return new QuantifiedParticipant(Quantifier.ALL, attributes);
	}

}
