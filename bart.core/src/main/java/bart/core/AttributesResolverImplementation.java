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
	private int policyIndex;

	public AttributesResolverImplementation(Request request, ContextHandler contextHandler, Policies policies, int policyIndex) {
		this.request = request;
		this.contextHandler = contextHandler;
		this.policies = policies;
		this.policyIndex = policyIndex;
	}

	@Override
	public Object name(String name) throws UndefinedName {
		return Stream.of(
					request.resource(),
					contextHandler.ofParty(policyIndex),
					policies.getByIndex(request.requester().index()).party())
			.map(attributes -> attributes.name(name))
			.filter(Objects::nonNull)
			.findFirst()
			.orElseThrow(() -> new UndefinedName(name));
	}
}