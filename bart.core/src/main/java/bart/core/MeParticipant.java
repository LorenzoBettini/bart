package bart.core;

public interface MeParticipant extends ExchangeToParticipant {

	@Override
	default boolean isMe() {
		return true;
	}

}
