package colpo.core;

/**
 * Default implementation of "comply": same requester, same from, and the
 * newRequest must match with the existing one.
 *
 * @author Lorenzo Bettini
 */
public class DefaultRequestComply implements RequestComply {

	private AttributeMatcher matcher;

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
