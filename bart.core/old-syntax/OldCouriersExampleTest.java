package colpo.core.examples;

import static colpo.core.Participants.anySuchThat;
import static colpo.core.Participants.index;
import static colpo.core.Participants.me;
import static colpo.core.Participants.requester;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import colpo.core.AndExchange;
import colpo.core.Attributes;
import colpo.core.ContextHandler;
import colpo.core.ExpressionWithDescription;
import colpo.core.OrExchange;
import colpo.core.Policies;
import colpo.core.Policy;
import colpo.core.Request;
import colpo.core.Rule;
import colpo.core.Rules;
import colpo.core.SingleExchange;
import colpo.core.semantics.Semantics;

class CouriersExampleTest {

	private Semantics semantics;
	private Policies policies;

	@BeforeEach
	void init() {
		policies = new Policies();
		semantics = new Semantics(policies);
	}

	@Test
	void firstScenario() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("service", "delivery")
					.add("company", "RabbitService"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Lucca")
					))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("service", "delivery")
					.add("company", "FastAndFurious"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Prato"),
						new OrExchange(
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Pistoia"),
								new Attributes(),
								requester()),
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Lucca"),
								new Attributes(),
								requester())
						)
					))));

		assertResultTrue(
			new Request(
				index(1), // RabbitService
				new Attributes()
					.add("type", "addrInfo")
					.add("city", "Prato"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("service", "delivery")
					.add("company", "FastAndFurious"))
			),
			"""
			evaluating Request[requester=1, resource=[(type : addrInfo), (city : Prato)], credentials=[], from=anySuchThat: [(service : delivery), (company : FastAndFurious)]]
			  finding matching policies
			    policy 2: from match([(service : delivery), (company : FastAndFurious)], [(service : delivery), (company : FastAndFurious)]) -> true
			  policy 2: evaluating Request[requester=1, resource=[(type : addrInfo), (city : Prato)], credentials=[], from=2]
			    rule 2.1: resource match([(type : addrInfo), (city : Prato)], [(type : addrInfo), (city : Prato)]) -> true
			    rule 2.1: condition true -> true
			    rule 2.1: evaluating OR(Exchange[to=ME, resource=[(type : addrInfo), (city : Pistoia)], credentials=[], from=REQUESTER], Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], credentials=[], from=REQUESTER])
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Pistoia)], credentials=[], from=REQUESTER]
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pistoia)], credentials=[], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pistoia)], credentials=[], from=1]
			          rule 1.1: resource match([(type : addrInfo), (city : Pistoia)], [(type : addrInfo), (city : Lucca)]) -> false
			      result: false
			    rule 2.1: OR
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], credentials=[], from=REQUESTER]
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], credentials=[], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], credentials=[], from=1]
			          rule 1.1: resource match([(type : addrInfo), (city : Lucca)], [(type : addrInfo), (city : Lucca)]) -> true
			          rule 1.1: condition true -> true
			      result: true
			    rule 2.1: END Exchange -> true
			result: true
			"""
		);
	}

	@Test
	void secondScenario() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("service", "delivery")
					.add("company", "RabbitService"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Lucca"),
						new ExpressionWithDescription(
							attributes -> attributes.name("affiliation").equals("RabbitService"),
							"affiliation = RabbitService")))
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Lucca"),
						new ExpressionWithDescription(
							attributes -> !attributes.name("affiliation").equals("RabbitService"),
							"affiliation != RabbitService"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("type", "addrInfo")
								.add("city", "Prato"),
							new Attributes()
								.add("affiliation", "RabbitService"),
							requester())))
					))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("service", "delivery")
					.add("company", "FastAndFurious"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Prato"),
						new OrExchange(
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Pistoia"),
								new Attributes()
									.add("affiliation", "FastAndFurious"),
								requester()),
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Lucca"),
								new Attributes()
									.add("affiliation", "FastAndFurious"),
								requester())
						)
					))));
		assertPolicies("""
		1 = Policy[party=[(service : delivery), (company : RabbitService)], rules=[resource=[(type : addrInfo), (city : Lucca)], condition=affiliation = RabbitService, resource=[(type : addrInfo), (city : Lucca)], condition=affiliation != RabbitService, exchange=Exchange[to=ME, resource=[(type : addrInfo), (city : Prato)], credentials=[(affiliation : RabbitService)], from=REQUESTER]]]
		2 = Policy[party=[(service : delivery), (company : FastAndFurious)], rules=[resource=[(type : addrInfo), (city : Prato)], condition=true, exchange=OR(Exchange[to=ME, resource=[(type : addrInfo), (city : Pistoia)], credentials=[(affiliation : FastAndFurious)], from=REQUESTER], Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], credentials=[(affiliation : FastAndFurious)], from=REQUESTER])]]
		""");
		assertResultTrue(
			new Request(
				index(1), // RabbitService
				new Attributes()
					.add("type", "addrInfo")
					.add("city", "Prato"),
				new Attributes()
					.add("affiliation", "RabbitService"),
				anySuchThat(new Attributes()
					.add("service", "delivery")
					.add("company", "FastAndFurious"))
			),
			"""
			evaluating Request[requester=1, resource=[(type : addrInfo), (city : Prato)], credentials=[(affiliation : RabbitService)], from=anySuchThat: [(service : delivery), (company : FastAndFurious)]]
			  finding matching policies
			    policy 2: from match([(service : delivery), (company : FastAndFurious)], [(service : delivery), (company : FastAndFurious)]) -> true
			  policy 2: evaluating Request[requester=1, resource=[(type : addrInfo), (city : Prato)], credentials=[(affiliation : RabbitService)], from=2]
			    rule 2.1: resource match([(type : addrInfo), (city : Prato)], [(type : addrInfo), (city : Prato)]) -> true
			    rule 2.1: condition true -> true
			    rule 2.1: evaluating OR(Exchange[to=ME, resource=[(type : addrInfo), (city : Pistoia)], credentials=[(affiliation : FastAndFurious)], from=REQUESTER], Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], credentials=[(affiliation : FastAndFurious)], from=REQUESTER])
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Pistoia)], credentials=[(affiliation : FastAndFurious)], from=REQUESTER]
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pistoia)], credentials=[(affiliation : FastAndFurious)], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pistoia)], credentials=[(affiliation : FastAndFurious)], from=1]
			          rule 1.1: resource match([(type : addrInfo), (city : Pistoia)], [(type : addrInfo), (city : Lucca)]) -> false
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pistoia)], credentials=[(affiliation : FastAndFurious)], from=1]
			          rule 1.2: resource match([(type : addrInfo), (city : Pistoia)], [(type : addrInfo), (city : Lucca)]) -> false
			      result: false
			    rule 2.1: OR
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], credentials=[(affiliation : FastAndFurious)], from=REQUESTER]
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], credentials=[(affiliation : FastAndFurious)], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], credentials=[(affiliation : FastAndFurious)], from=1]
			          rule 1.1: resource match([(type : addrInfo), (city : Lucca)], [(type : addrInfo), (city : Lucca)]) -> true
			          rule 1.1: condition affiliation = RabbitService -> false
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], credentials=[(affiliation : FastAndFurious)], from=1]
			          rule 1.2: resource match([(type : addrInfo), (city : Lucca)], [(type : addrInfo), (city : Lucca)]) -> true
			          rule 1.2: condition affiliation != RabbitService -> true
			          rule 1.2: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Prato)], credentials=[(affiliation : RabbitService)], from=REQUESTER]
			          rule 1.2: already found Request[requester=1, resource=[(type : addrInfo), (city : Prato)], credentials=[(affiliation : RabbitService)], from=2]
			      result: true
			    rule 2.1: END Exchange -> true
			result: true
			"""
		);
	}

	@Test
	void thirdScenario() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("service", "delivery")
					.add("company", "RabbitService"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Lucca"),
						new ExpressionWithDescription(
							c -> c.name("affiliation").equals("RabbitService"),
							"affiliation = RabbitService")))
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Lucca"),
						new ExpressionWithDescription(
							c -> !c.name("affiliation").equals("RabbitService"),
							"affiliation != RabbitService"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("type", "addrInfo")
								.add("city", "Prato"),
							new Attributes()
								.add("affiliation", "RabbitService"),
							requester())))
					))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("service", "delivery")
					.add("company", "FastAndFurious"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Prato"),
						new AndExchange(
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Lucca"),
								new Attributes()
									.add("affiliation", "FastAndFurious"),
								anySuchThat(new Attributes()
									.add("service", "delivery")
									.add("company", "RabbitService"))),
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Grosseto"),
								new Attributes()
									.add("affiliation", "FastAndFurious"),
								anySuchThat(new Attributes()
										.add("service", "delivery")
										.add("company", "RabbitService")))
						)
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("service", "delivery")
					.add("company", "RabbitService"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Grosseto")
					))));
		assertPolicies("""
		1 = Policy[party=[(service : delivery), (company : RabbitService)], rules=[resource=[(type : addrInfo), (city : Lucca)], condition=affiliation = RabbitService, resource=[(type : addrInfo), (city : Lucca)], condition=affiliation != RabbitService, exchange=Exchange[to=ME, resource=[(type : addrInfo), (city : Prato)], credentials=[(affiliation : RabbitService)], from=REQUESTER]]]
		2 = Policy[party=[(service : delivery), (company : FastAndFurious)], rules=[resource=[(type : addrInfo), (city : Prato)], condition=true, exchange=AND(Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], credentials=[(affiliation : FastAndFurious)], from=anySuchThat: [(service : delivery), (company : RabbitService)]], Exchange[to=ME, resource=[(type : addrInfo), (city : Grosseto)], credentials=[(affiliation : FastAndFurious)], from=anySuchThat: [(service : delivery), (company : RabbitService)]])]]
		3 = Policy[party=[(service : delivery), (company : RabbitService)], rules=[resource=[(type : addrInfo), (city : Grosseto)], condition=true]]
		""");
		assertResultTrue(
			new Request(
				index(1), // RabbitService
				new Attributes()
					.add("type", "addrInfo")
					.add("city", "Prato"),
				new Attributes()
					.add("affiliation", "RabbitService"),
				anySuchThat(new Attributes()
					.add("service", "delivery")
					.add("company", "FastAndFurious"))
			),
			"""
			evaluating Request[requester=1, resource=[(type : addrInfo), (city : Prato)], credentials=[(affiliation : RabbitService)], from=anySuchThat: [(service : delivery), (company : FastAndFurious)]]
			  finding matching policies
			    policy 2: from match([(service : delivery), (company : FastAndFurious)], [(service : delivery), (company : FastAndFurious)]) -> true
			    policy 3: from match([(service : delivery), (company : FastAndFurious)], [(service : delivery), (company : RabbitService)]) -> false
			  policy 2: evaluating Request[requester=1, resource=[(type : addrInfo), (city : Prato)], credentials=[(affiliation : RabbitService)], from=2]
			    rule 2.1: resource match([(type : addrInfo), (city : Prato)], [(type : addrInfo), (city : Prato)]) -> true
			    rule 2.1: condition true -> true
			    rule 2.1: evaluating AND(Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], credentials=[(affiliation : FastAndFurious)], from=anySuchThat: [(service : delivery), (company : RabbitService)]], Exchange[to=ME, resource=[(type : addrInfo), (city : Grosseto)], credentials=[(affiliation : FastAndFurious)], from=anySuchThat: [(service : delivery), (company : RabbitService)]])
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Lucca)], credentials=[(affiliation : FastAndFurious)], from=anySuchThat: [(service : delivery), (company : RabbitService)]]
			      policy 1: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : RabbitService)]) -> true
			      policy 2: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : FastAndFurious)]) -> false
			      policy 3: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : RabbitService)]) -> true
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], credentials=[(affiliation : FastAndFurious)], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], credentials=[(affiliation : FastAndFurious)], from=1]
			          rule 1.1: resource match([(type : addrInfo), (city : Lucca)], [(type : addrInfo), (city : Lucca)]) -> true
			          rule 1.1: condition affiliation = RabbitService -> false
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Lucca)], credentials=[(affiliation : FastAndFurious)], from=1]
			          rule 1.2: resource match([(type : addrInfo), (city : Lucca)], [(type : addrInfo), (city : Lucca)]) -> true
			          rule 1.2: condition affiliation != RabbitService -> true
			          rule 1.2: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Prato)], credentials=[(affiliation : RabbitService)], from=REQUESTER]
			          rule 1.2: already found Request[requester=1, resource=[(type : addrInfo), (city : Prato)], credentials=[(affiliation : RabbitService)], from=2]
			      result: true
			    rule 2.1: AND
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Grosseto)], credentials=[(affiliation : FastAndFurious)], from=anySuchThat: [(service : delivery), (company : RabbitService)]]
			      policy 1: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : RabbitService)]) -> true
			      policy 2: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : FastAndFurious)]) -> false
			      policy 3: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : RabbitService)]) -> true
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Grosseto)], credentials=[(affiliation : FastAndFurious)], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Grosseto)], credentials=[(affiliation : FastAndFurious)], from=1]
			          rule 1.1: resource match([(type : addrInfo), (city : Grosseto)], [(type : addrInfo), (city : Lucca)]) -> false
			        policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Grosseto)], credentials=[(affiliation : FastAndFurious)], from=1]
			          rule 1.2: resource match([(type : addrInfo), (city : Grosseto)], [(type : addrInfo), (city : Lucca)]) -> false
			      result: false
			      evaluating Request[requester=2, resource=[(type : addrInfo), (city : Grosseto)], credentials=[(affiliation : FastAndFurious)], from=3]
			        policy 3: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Grosseto)], credentials=[(affiliation : FastAndFurious)], from=3]
			          rule 3.1: resource match([(type : addrInfo), (city : Grosseto)], [(type : addrInfo), (city : Grosseto)]) -> true
			          rule 3.1: condition true -> true
			      result: true
			    rule 2.1: END Exchange -> true
			result: true
			"""
		);
	}

	@Test
	void fourthScenario() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("service", "delivery")
					.add("company", "RabbitService"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Lucca"),
						new ExpressionWithDescription(
							c -> c.name("affiliation").equals("RabbitService"),
							"affiliation = RabbitService")))
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Lucca"),
						new ExpressionWithDescription(
							c -> !c.name("affiliation").equals("RabbitService"),
							"affiliation != RabbitService"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("type", "addrInfo")
								.add("city", "Prato"),
							new Attributes()
								.add("affiliation", "RabbitService"),
							requester())))
					))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("service", "delivery")
					.add("company", "FastAndFurious"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Prato"),
						new AndExchange(
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Lucca"),
								new Attributes()
									.add("affiliation", "FastAndFurious"),
								anySuchThat(new Attributes()
									.add("service", "delivery")
									.add("company", "RabbitService"))),
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "addrInfo")
									.add("city", "Grosseto"),
								new Attributes()
									.add("affiliation", "FastAndFurious"),
								anySuchThat(new Attributes()
										.add("service", "delivery")
										.add("company", "RabbitService")))
						)))
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Firenze"),
						new ExpressionWithDescription(
							attributes -> {
								var timeHour = (int) attributes.name("timeHour");
								return timeHour > 7 && timeHour < 20 &&
									attributes.name("position").equals("Firenze");
							},
							"timeHour > 7 and timeHour < 20 and position = Firenze"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("type", "addrInfo")
								.add("city", "Pisa"),
							new Attributes()
								.add("affiliation", "FastAndFurious"),
							anySuchThat(new Attributes()
									.add("service", "delivery")
									.add("company", "RabbitService")))
						))
				))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("service", "delivery")
					.add("company", "RabbitService"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Grosseto")
					))
					.add(new Rule(
						new Attributes()
							.add("type", "addrInfo")
							.add("city", "Pisa"),
						new SingleExchange(
							anySuchThat(new Attributes()
									.add("service", "delivery")
									.add("company", "RabbitService")),
							new Attributes()
								.add("type", "addrInfo")
								.add("city", "Firenze"),
							new Attributes()
								.add("affiliation", "RabbitService"),
							requester())
					))
				));
		// don't assert policies for simplicity and readability

		// add environmental information to context handler for the first two parties
		semantics.contextHandler(new ContextHandler()
			.add(1, "timeHour", 10)
			.add(1, "position", "Lucca")
			.add(2, "timeHour", 10)
			.add(2, "position", "Firenze")
		);

		assertResultTrue(
			new Request(
				index(1), // RabbitService
				new Attributes()
					.add("type", "addrInfo")
					.add("city", "Firenze"),
				new Attributes()
					.add("affiliation", "RabbitService"),
				anySuchThat(new Attributes()
					.add("service", "delivery")
					.add("company", "FastAndFurious"))
			),
			"""
			evaluating Request[requester=1, resource=[(type : addrInfo), (city : Firenze)], credentials=[(affiliation : RabbitService)], from=anySuchThat: [(service : delivery), (company : FastAndFurious)]]
			  finding matching policies
			    policy 2: from match([(service : delivery), (company : FastAndFurious)], [(service : delivery), (company : FastAndFurious)]) -> true
			    policy 3: from match([(service : delivery), (company : FastAndFurious)], [(service : delivery), (company : RabbitService)]) -> false
			  policy 2: evaluating Request[requester=1, resource=[(type : addrInfo), (city : Firenze)], credentials=[(affiliation : RabbitService)], from=2]
			    rule 2.1: resource match([(type : addrInfo), (city : Firenze)], [(type : addrInfo), (city : Prato)]) -> false
			  policy 2: evaluating Request[requester=1, resource=[(type : addrInfo), (city : Firenze)], credentials=[(affiliation : RabbitService)], from=2]
			    rule 2.2: resource match([(type : addrInfo), (city : Firenze)], [(type : addrInfo), (city : Firenze)]) -> true
			    rule 2.2: condition timeHour > 7 and timeHour < 20 and position = Firenze -> true
			    rule 2.2: evaluating Exchange[to=ME, resource=[(type : addrInfo), (city : Pisa)], credentials=[(affiliation : FastAndFurious)], from=anySuchThat: [(service : delivery), (company : RabbitService)]]
			    policy 1: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : RabbitService)]) -> true
			    policy 2: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : FastAndFurious)]) -> false
			    policy 3: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : RabbitService)]) -> true
			    evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pisa)], credentials=[(affiliation : FastAndFurious)], from=1]
			      policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pisa)], credentials=[(affiliation : FastAndFurious)], from=1]
			        rule 1.1: resource match([(type : addrInfo), (city : Pisa)], [(type : addrInfo), (city : Lucca)]) -> false
			      policy 1: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pisa)], credentials=[(affiliation : FastAndFurious)], from=1]
			        rule 1.2: resource match([(type : addrInfo), (city : Pisa)], [(type : addrInfo), (city : Lucca)]) -> false
			    result: false
			    evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pisa)], credentials=[(affiliation : FastAndFurious)], from=3]
			      policy 3: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pisa)], credentials=[(affiliation : FastAndFurious)], from=3]
			        rule 3.1: resource match([(type : addrInfo), (city : Pisa)], [(type : addrInfo), (city : Grosseto)]) -> false
			      policy 3: evaluating Request[requester=2, resource=[(type : addrInfo), (city : Pisa)], credentials=[(affiliation : FastAndFurious)], from=3]
			        rule 3.2: resource match([(type : addrInfo), (city : Pisa)], [(type : addrInfo), (city : Pisa)]) -> true
			        rule 3.2: condition true -> true
			        rule 3.2: evaluating Exchange[to=anySuchThat: [(service : delivery), (company : RabbitService)], resource=[(type : addrInfo), (city : Firenze)], credentials=[(affiliation : RabbitService)], from=REQUESTER]
			        policy 1: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : RabbitService)]) -> true
			        policy 2: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : FastAndFurious)]) -> false
			        policy 3: from match([(service : delivery), (company : RabbitService)], [(service : delivery), (company : RabbitService)]) -> true
			        rule 3.2: already found Request[requester=1, resource=[(type : addrInfo), (city : Firenze)], credentials=[(affiliation : RabbitService)], from=2]
			    result: true
			result: true
			"""
		);
	}

	private void assertPolicies(String expected) {
		assertEquals(expected, policies.description());
	}

	private void assertResultTrue(Request request, String expectedTrace) {
		assertAll(
			() -> assertTrue(semantics.evaluate(request)),
			() -> assertEquals(expectedTrace, semantics.getTrace().toString())
		);
	}

}
