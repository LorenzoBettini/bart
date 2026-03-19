package bart.core;

/**
 * Default implementation of {@link RequestComply} that considers two requests
 * compliant when they share the same requester, the same {@code from} party,
 * and the new request's resource attributes are a subset match of the existing
 * request's resource attributes.
 *
 * @author Lorenzo Bettini
 */
public class DefaultRequestComply implements RequestComply {

	private AttributeMatcher matcher;

	/**
	 * Constructs a new {@code DefaultRequestComply} using the given matcher.
	 *
	 * @param matcher the attribute matcher used to compare resources
	 */
	public DefaultRequestComply(AttributeMatcher matcher) {
		this.matcher = matcher;
	}

	@Override
	public boolean test(Request newRequest, Request existingRequest) {
		return newRequest.requester().equals(existingRequest.requester())
				&& newRequest.from().equals(existingRequest.from())
				&& matcher.match(newRequest.resource(), existingRequest.resource());
	}

}
