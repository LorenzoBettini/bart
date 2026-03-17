/**
 *
 */
package bart.core;

import bart.core.semantics.UndefinedName;

/**
 * @author Lorenzo Bettini
 */
public interface NameResolver {

	Object name(String name) throws UndefinedName;

	<T> T name(String name, Class<T> clazz) throws UndefinedName;

	Object nameFromRequester(String name) throws UndefinedName;

	Object nameFromParty(String name, Attributes attributes) throws UndefinedName;
}
