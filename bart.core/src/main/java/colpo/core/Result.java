package colpo.core;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The result of an evaluation.
 *
 * @author Lorenzo Bettini
 */
public class Result {

	private boolean permitted = false;
	private Collection<Request> requests = new ArrayList<>();

	public Result(boolean permitted) {
		this.permitted = permitted;
	}

	public static Result permitted() {
		return new Result(true);
	}

	public boolean isPermitted() {
		return permitted;
	}

	public Collection<Request> getRequests() {
		return requests;
	}

	public Result add(Request request) {
		requests.add(request);
		return this;
	}

	public Result addAll(Collection<Request> requests) {
		this.requests.addAll(requests);
		return this;
	}
}
