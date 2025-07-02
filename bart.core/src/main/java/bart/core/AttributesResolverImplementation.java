package bart.core;

import java.util.Objects;
import java.util.stream.Stream;

import bart.core.semantics.UndefinedName;

/**
 * Implementation of the {@link AttributesResolver} interface that resolves
 * attributes from a request, context handler, and policies.
 * 
 * @author Lorenzo Bettini
 */
public final class AttributesResolverImplementation implements AttributesResolver {

	private Request request;
	private ContextHandler contextHandler;
	private Policies policies;

	public AttributesResolverImplementation(Request request, ContextHandler contextHandler, Policies policies) {
		this.request = request;
		this.contextHandler = contextHandler;
		this.policies = policies;
	}

	@Override
	public Object name(String name) throws UndefinedName {
		var index = request.from().getIndex();
		return retrieveName(name, contextHandler.ofParty(index), policies.getByIndex(index).party());
	}

	@Override
	public Object nameFromRequester(String name) throws UndefinedName {
		var index = request.requester().index();
		return retrieveName(name, contextHandler.ofParty(index), policies.getByIndex(index).party());
	}

	private Object retrieveName(String name, Attributes fromContext, Attributes fromParty) throws UndefinedName {
		return Stream.of(
					request.resource(),
					fromContext,
					fromParty)
			.map(attributes -> attributes.name(name))
			.filter(Objects::nonNull)
			.findFirst()
			.orElseThrow(() -> new UndefinedName(name));
	}

}