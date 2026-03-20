package bart.core;

/**
 * Represents the policy of a single participant (party).
 * <p>
 * A policy pairs the party's identifying {@link Attributes} (e.g. name, role)
 * with a set of {@link Rules} that define under what conditions the party will
 * provide resources and what it requires in exchange.
 * </p>
 *
 * <p>Example:
 * {@snippet :
 * var policy = new Policy(
 *     new Attributes().add("name", "Alice").add("role", "Provider"),
 *     new Rules()
 *         .add(new Rule(
 *             new Attributes().add("resource/type", "printer"),
 *             new SingleExchange(Participants.me(),
 *                 new Attributes().add("resource/type", "paper"),
 *                 Participants.requester()))));
 * }
 * </p>
 *
 * @param party the attributes identifying this participant
 * @param rules the rules governing resource access for this participant
 * @author Lorenzo Bettini
 */
public record Policy(Attributes party, Rules rules) {

}
