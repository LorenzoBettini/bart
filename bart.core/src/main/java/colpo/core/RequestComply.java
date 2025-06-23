package colpo.core;

/**
 * Given a request created for an exchange and an existing request in the
 * collected ones, the predicate has to decide whether the existing request can
 * be considered compliant with the new one created for the exchange.
 *
 * @author Lorenzo Bettini
 */
@FunctionalInterface
public interface RequestComply {
	boolean test(Request newRequest, Request existingRequest);
}
