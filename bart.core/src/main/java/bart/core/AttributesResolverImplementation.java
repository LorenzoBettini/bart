package bart.core;

import java.util.Objects;
import java.util.stream.Stream;

import bart.core.semantics.UndefinedName;

/**
 * Implementation of the {@link AttributesResolver} interface that resolves
 * attributes from a request, context handler, and policies.
 * <p>
 * This class provides methods to resolve attribute values by searching in the following order:
 * <ol>
 *   <li>Request resource attributes</li>
 *   <li>Context handler attributes for a specific party</li>
 *   <li>Party attributes from the relevant policy</li>
 * </ol>
 * The resolution order and the party/context used depend on the method called.
 * </p>
 *
 * @author Lorenzo Bettini
 */
public final class AttributesResolverImplementation implements AttributesResolver {

	private Request request;
	private ContextHandler contextHandler;
	private Policies policies;

	/**
	 * Constructs a new resolver for the given request, context handler, and policies.
	 *
	 * @param request the resource request
	 * @param contextHandler the context handler providing per-party attributes
	 * @param policies the set of policies (indexed from 1)
	 */
	public AttributesResolverImplementation(Request request, ContextHandler contextHandler, Policies policies) {
		this.request = request;
		this.contextHandler = contextHandler;
		this.policies = policies;
	}

	/**
	 * Resolves the value of the given attribute name for the party specified by {@code request.from()}.
	 * <p>
	 * The search order is: resource attributes, context handler for the 'from' index, then party attributes for the 'from' index.
	 * </p>
	 *
	 * @param name the attribute name to resolve
	 * @return the resolved attribute value
	 * @throws UndefinedName if the attribute cannot be found
	 */
	@Override
	public Object name(String name) throws UndefinedName {
		var index = request.from().getIndex();
		return retrieveName(name, contextHandler.ofParty(index), policies.getByIndex(index).party());
	}

	/**
	 * Resolves the value of the given attribute name for the party specified by {@code request.from()}
	 * and casts it to the specified type.
	 * <p>
	 * The search order is: resource attributes, context handler for the 'from' index, then party attributes for the 'from' index.
	 * </p>
	 *
	 * @param <T> the type to cast the result to
	 * @param name the attribute name to resolve
	 * @param clazz the class to cast the result to
	 * @return the resolved attribute value cast to the specified type
	 * @throws UndefinedName if the attribute cannot be found
	 * @throws ClassCastException if the resolved value cannot be cast to the specified type
	 */
	@Override
	public <T> T name(String name, Class<T> clazz) throws UndefinedName {
		return clazz.cast(name(name));
	}

	/**
	 * Resolves the value of the given attribute name for the party specified by {@code request.requester()}.
	 * <p>
	 * The search order is: resource attributes, context handler for the requester index, then party attributes for the requester index.
	 * </p>
	 *
	 * @param name the attribute name to resolve
	 * @return the resolved attribute value
	 * @throws UndefinedName if the attribute cannot be found
	 */
	@Override
	public Object nameFromRequester(String name) throws UndefinedName {
		var index = request.requester().index();
		return retrieveName(name, contextHandler.ofParty(index), policies.getByIndex(index).party());
	}

	/**
	 * Resolves the value of the given attribute name for the first party whose attributes match the given search attributes.
	 * <p>
	 * The search order is: resource attributes, context handler for the first matching party, then party attributes for the first matching party.
	 * If no party matches, empty context and party attributes are used.
	 * </p>
	 *
	 * @param name the attribute name to resolve
	 * @param attributes the attributes to match against party attributes
	 * @return the resolved attribute value
	 * @throws UndefinedName if the attribute cannot be found in any source
	 */
	@Override
	public Object nameFromParty(String name, Attributes attributes) throws UndefinedName {
		var attributeMatcher = new AttributeMatcher();
		var firstMatchingParty = policies.getPolicyData()
			.filter(d -> attributeMatcher.match(attributes, d.policy().party()))
			.findFirst();
		var fromContext = new Attributes();
		var fromParty = new Attributes();
		if (firstMatchingParty.isPresent()) {
			var policyData = firstMatchingParty.get();
			fromContext = contextHandler.ofParty(policyData.index());
			fromParty = policyData.policy().party();
		}
		return retrieveName(name, fromContext, fromParty);
	}

	/**
	 * Helper method to resolve the value of the given attribute name from the provided sources in order:
	 * <ol>
	 *   <li>request resource attributes</li>
	 *   <li>fromContext</li>
	 *   <li>fromParty</li>
	 * </ol>
	 * Returns the first non-null value found, or throws {@link UndefinedName} if not found.
	 *
	 * @param name the attribute name to resolve
	 * @param fromContext the context handler attributes for the relevant party
	 * @param fromParty the party attributes from the relevant policy
	 * @return the resolved attribute value
	 * @throws UndefinedName if the attribute cannot be found in any source
	 */
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