/**
 *
 */
package colpo.core.semantics;

/**
 * @author Lorenzo Bettini
 */
public class UndefinedName extends Exception {

	private static final long serialVersionUID = 1L;

	public UndefinedName(String name) {
		super("Undefined name: " + name);
	}

}
