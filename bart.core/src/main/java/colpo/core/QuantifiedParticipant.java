package colpo.core;

import java.util.Objects;

/**
 * @author Lorenzo Bettini
 */
public class QuantifiedParticipant implements RequestFromParticipant, ExchangeToParticipant, ExchangeFromParticipant {

	private Quantifier quantifier = Quantifier.UNSET;
	private Attributes attributes;

	public enum Quantifier {
		UNSET, ANY, ALL
	}

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
			(quantifier == Quantifier.ANY ? "anySuchThat" : "allSuchThat"),
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
