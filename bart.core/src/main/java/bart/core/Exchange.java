package bart.core;

/**
 * Sealed interface representing an exchange in the Bart framework.
 * <p>
 * An exchange specifies the resource transfer that a policy requires in return
 * for granting access. Exchanges can be:
 * <ul>
 *   <li>{@link SingleExchange} — a direct transfer from one party to another</li>
 *   <li>{@link AndExchange} — both left and right exchanges must succeed</li>
 *   <li>{@link OrExchange} — either left or right exchange can succeed</li>
 * </ul>
 * </p>
 *
 * @see SingleExchange
 * @see AndExchange
 * @see OrExchange
 * @author Lorenzo Bettini
 */
public sealed interface Exchange permits SingleExchange, CompositeExchange {

}