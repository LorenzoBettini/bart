/**
 *
 */
package bart.core.semantics;

/**
 * Thrown by {@link bart.core.NameResolver} when a requested attribute name
 * cannot be resolved in any of the available lookup sources (request resource,
 * context handler, or party attributes).
 *
 * @author Lorenzo Bettini
 */
public class UndefinedName extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new {@code UndefinedName} exception for the given attribute name.
	 *
	 * @param name the attribute name that could not be resolved
	 */
	public UndefinedName(String name) {
		super("Undefined name: " + name);
	}

}
