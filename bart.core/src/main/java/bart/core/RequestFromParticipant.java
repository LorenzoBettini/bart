package bart.core;

/**
 * Represents a participant specification usable as the {@code from} party in a
 * {@link Request}.
 * <p>
 * Implementations include {@link IndexParticipant} (a concrete party index)
 * and {@link QuantifiedParticipant} (matching by attributes at evaluation time).
 * </p>
 *
 * @see Request
 * @author Lorenzo Bettini
 */
public interface RequestFromParticipant extends Participant {

}
