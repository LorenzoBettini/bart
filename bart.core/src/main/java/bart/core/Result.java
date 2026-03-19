package bart.core;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The result of evaluating a {@link Request} against the current set of policies.
 * <p>
 * A result is either <em>permitted</em> or <em>denied</em>.  When permitted, it
 * also carries the complete set of sub-{@link Request}s that were generated and
 * satisfied during the evaluation (i.e. the exchange chain that enables access).
 * </p>
 *
 * @author Lorenzo Bettini
 */
public class Result {

	private boolean permitted = false;
	private Collection<Request> requests = new ArrayList<>();

	/**
	 * Creates a new result with the given permission flag and an empty request list.
	 *
	 * @param permitted {@code true} if access is permitted, {@code false} if denied
	 */
	public Result(boolean permitted) {
		this.permitted = permitted;
	}

	/**
	 * Returns a new permitted result with an empty request list.
	 *
	 * @return a permitted {@code Result}
	 */
	public static Result permitted() {
		return new Result(true);
	}

	/**
	 * Returns {@code true} if the evaluated request is permitted.
	 *
	 * @return {@code true} if permitted
	 */
	public boolean isPermitted() {
		return permitted;
	}

	/**
	 * Returns the collection of requests that were generated and satisfied during
	 * evaluation to enable this result.
	 *
	 * @return the satisfied sub-requests; may be empty, never {@code null}
	 */
	public Collection<Request> getRequests() {
		return requests;
	}

	/**
	 * Adds a single request to the satisfied requests of this result.
	 *
	 * @param request the request to add
	 * @return {@code this} to allow fluent chaining
	 */
	public Result add(Request request) {
		requests.add(request);
		return this;
	}

	/**
	 * Adds all requests from the given collection to the satisfied requests of
	 * this result.
	 *
	 * @param requests the requests to add
	 * @return {@code this} to allow fluent chaining
	 */
	public Result addAll(Collection<Request> requests) {
		this.requests.addAll(requests);
		return this;
	}
}
