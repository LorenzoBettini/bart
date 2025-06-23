package colpo.core;

import colpo.core.QuantifiedParticipant.Quantifier;

/**
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

	public static IndexParticipant index(int index) {
		return new IndexParticipant(index);
	}

	public static MeParticipant me() {
		return ME;
	}

	public static RequesterParticipant requester() {
		return REQUESTER;
	}

	public static QuantifiedParticipant anySuchThat(Attributes attributes) {
		return new QuantifiedParticipant(Quantifier.ANY, attributes);
	}

	public static QuantifiedParticipant allSuchThat(Attributes attributes) {
		return new QuantifiedParticipant(Quantifier.ALL, attributes);
	}

}
