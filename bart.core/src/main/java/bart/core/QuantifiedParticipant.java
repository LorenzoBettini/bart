package bart.core;

import java.util.Objects;

/**
 * A participant that matches a set of parties by their attributes using a
 * quantifier: {@code ANY} (at least one) or {@code ALL} (every matching party).
 * <p>
 * Quantified participants can appear as the requester's target in a request, or
 * as either the source or target of an exchange.
 * Use the factory methods {@link Participants#any(Attributes)} and
 * {@link Participants#all(Attributes)} to create instances.
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * // Matches any party with role "Provider"
 * var provider = Participants.any(new Attributes().add("role", "Provider"));
 * // Matches all parties with role "Courier"
 * var allCouriers = Participants.all(new Attributes().add("role", "Courier"));
 * }
 * </p>
 *
 * @author Lorenzo Bettini
 */
public class QuantifiedParticipant implements RequestFromParticipant, ExchangeToParticipant, ExchangeFromParticipant {

	private Quantifier quantifier = Quantifier.UNSET;
	private Attributes attributes;

	/**
	 * The quantifier applied when evaluating how many matching parties must
	 * satisfy the exchange or request.
	 */
	public enum Quantifier {
		/** No quantifier set (default / unresolved). */
		UNSET,
		/** At least one matching party must satisfy the condition. */
		ANY,
		/** Every matching party must satisfy the condition. */
		ALL
	}

	/**
	 * Creates a new quantified participant.
	 *
	 * @param quantifier the quantifier ({@link Quantifier#ANY} or {@link Quantifier#ALL})
	 * @param attributes the attributes used to match target parties
	 */
	public QuantifiedParticipant(Quantifier quantifier, Attributes attributes) {
		this.quantifier = quantifier;
		this.attributes = attributes;
	}

	@Override
	public int getIndex() {
		return -1;
	}

	@Override
	public boolean isAll() {
		return quantifier == Quantifier.ALL;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		return String.format("%s: %s",
			(quantifier == Quantifier.ANY ? "any" : "all"),
			attributes
		);
	}

	@Override
	public int hashCode() {
		return Objects.hash(attributes, quantifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		QuantifiedParticipant other = (QuantifiedParticipant) obj;
		return Objects.equals(attributes, other.attributes) &&
				quantifier == other.quantifier;
	}
}
