package colpo.core;

public interface Participant {

	static final Attributes EMPTY_ATTRIBUTES = new Attributes();

	default int getIndex() {
		return -1;
	}

	default boolean isAll() {
		return false;
	}

	default Attributes getAttributes() {
		return EMPTY_ATTRIBUTES;
	}
}
