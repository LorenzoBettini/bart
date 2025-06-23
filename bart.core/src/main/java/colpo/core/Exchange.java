package colpo.core;

/**
 * A sealed interface representing an exchange in the Bart system.
 * 
 * @author Lorenzo Bettini
 */
public sealed interface Exchange permits SingleExchange, CompositeExchange {

}