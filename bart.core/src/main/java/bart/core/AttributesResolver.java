/**
 *
 */
package bart.core;

import bart.core.semantics.UndefinedName;

/**
 * @author Lorenzo Bettini
 */
public interface AttributesResolver {

	Object name(String name) throws UndefinedName;

	Object nameFromRequester(String name) throws UndefinedName;

	Object nameFromParty(String name, Attributes attributes) throws UndefinedName;
}
