package bart.core;

public interface RequesterParticipant extends ExchangeFromParticipant {

	@Override
	default boolean isRequester() {
		return true;
	}
}
