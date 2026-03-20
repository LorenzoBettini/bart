/**
 *
 */
package bart.core;

import bart.core.semantics.UndefinedName;

/**
 * Resolves attribute values by name within the context of an ongoing semantic
 * evaluation.
 * <p>
 * A {@code NameResolver} is passed to {@link ExpressionCode#evaluate(NameResolver)}
 * so that condition expressions can query attributes from different participants
 * and the current context without having direct access to the full policy model.
 * Resolution is typically performed in the following lookup order:
 * <ol>
 *   <li>Request resource attributes</li>
 *   <li>Context-handler attributes for the relevant party</li>
 *   <li>Party attributes of the relevant policy</li>
 * </ol>
 * </p>
 *
 * @see ExpressionCode
 * @see NameResolverImplementation
 * @author Lorenzo Bettini
 */
public interface NameResolver {

	/**
	 * Resolves the attribute named {@code name} for the party that is the
	 * {@code from} side of the current request.
	 *
	 * @param name the attribute name
	 * @return the resolved value
	 * @throws UndefinedName if {@code name} cannot be found
	 */
	Object name(String name) throws UndefinedName;

	/**
	 * Resolves the attribute named {@code name} for the {@code from} party and
	 * casts it to the specified type.
	 *
	 * @param <T>   the expected type
	 * @param name  the attribute name
	 * @param clazz the class to cast to
	 * @return the resolved value cast to {@code T}
	 * @throws UndefinedName     if {@code name} cannot be found
	 * @throws ClassCastException if the value cannot be cast
	 */
	<T> T name(String name, Class<T> clazz) throws UndefinedName;

	/**
	 * Resolves the attribute named {@code name} for the {@code requester} of
	 * the current request.
	 *
	 * @param name the attribute name
	 * @return the resolved value
	 * @throws UndefinedName if {@code name} cannot be found
	 */
	Object nameFromRequester(String name) throws UndefinedName;

	/**
	 * Resolves the attribute named {@code name} for the {@code requester} and
	 * casts it to the specified type.
	 *
	 * @param <T>   the expected type
	 * @param name  the attribute name
	 * @param clazz the class to cast to
	 * @return the resolved value cast to {@code T}
	 * @throws UndefinedName     if {@code name} cannot be found
	 * @throws ClassCastException if the value cannot be cast
	 */
	<T> T nameFromRequester(String name, Class<T> clazz) throws UndefinedName;

	/**
	 * Resolves the attribute named {@code name} for the first party whose
	 * policy attributes match {@code attributes}.
	 *
	 * @param name       the attribute name
	 * @param attributes the attributes used to identify the target party
	 * @return the resolved value
	 * @throws UndefinedName if {@code name} cannot be found
	 */
	Object nameFromParty(String name, Attributes attributes) throws UndefinedName;

	/**
	 * Resolves the attribute named {@code name} for the first party whose
	 * policy attributes match {@code attributes} and casts it to the specified type.
	 *
	 * @param <T>        the expected type
	 * @param name       the attribute name
	 * @param attributes the attributes used to identify the target party
	 * @param clazz      the class to cast to
	 * @return the resolved value cast to {@code T}
	 * @throws UndefinedName     if {@code name} cannot be found
	 * @throws ClassCastException if the value cannot be cast
	 */
	<T> T nameFromParty(String name, Attributes attributes, Class<T> clazz) throws UndefinedName;
}
