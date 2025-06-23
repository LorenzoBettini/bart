package colpo.core;

/**
 * A sealed interface representing an exchange in the Colpo system.
 * 
 * @author Lorenzo Bettini
 */
public sealed interface Exchange permits SingleExchange, CompositeExchange {

}