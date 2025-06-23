package colpo.core;

/**
 * A sealed interface representing a composite exchange in the Colpo system.
 *
 * @author Lorenzo Bettini
 */
public sealed interface CompositeExchange extends Exchange permits AndExchange, OrExchange {

	Exchange left();

	Exchange right();
}