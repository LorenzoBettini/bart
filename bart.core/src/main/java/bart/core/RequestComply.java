package bart.core;

/**
 * A predicate that determines whether an existing request satisfies ("complies
 * with") a newly generated exchange request, allowing it to be reused instead
 * of being recursively evaluated again.
 * <p>
 * This interface is a key extension point: custom implementations allow
 * fine-tuning how the semantics engine de-duplicates exchange requests, e.g.
 * by relaxing or tightening attribute matching.
 * </p>
 *
 * @see DefaultRequestComply
 * @author Lorenzo Bettini
 */
@FunctionalInterface
public interface RequestComply {
	/**
	 * Returns {@code true} if {@code existingRequest} can be considered as
	 * satisfying {@code newRequest}.
	 *
	 * @param newRequest      the exchange request that needs to be satisfied
	 * @param existingRequest a previously collected request to check against
	 * @return {@code true} if the existing request complies with the new one
	 */
	boolean test(Request newRequest, Request existingRequest);
}
