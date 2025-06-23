package colpo.core.semantics;

import static colpo.core.Participants.allSuchThat;
import static colpo.core.Participants.anySuchThat;
import static colpo.core.Participants.index;
import static colpo.core.Participants.me;
import static colpo.core.Participants.requester;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;

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

class SemanticsTest {

	private Semantics semantics;
	private Policies policies;

	private static final ExpressionWithDescription FALSE =
			new ExpressionWithDescription(context -> false, "always false");

	@BeforeEach
	void init() {
		policies = new Policies();
		semantics = new Semantics(policies);
	}

	@Test
	void shouldCheckRequestFromWithIndexes() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice"),
				new Rules()
					.add(new Rule())))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob"),
				new Rules()
					.add(new Rule())))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl"),
				new Rules()
					.add(new Rule())));
		assertPolicies("""
		1 = Policy[party=[(name : Alice)], rules=[resource=[], condition=true]]
		2 = Policy[party=[(name : Bob)], rules=[resource=[], condition=true]]
		3 = Policy[party=[(name : Carl)], rules=[resource=[], condition=true]]
		""");
		// from as index is only generated using exchange
		// so this is just an internal test
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				index(2)
			),
			"""
			evaluating Request[requester=1, resource=[], from=2]
			  policy 2: evaluating Request[requester=1, resource=[], from=2]
			    rule 2.1: resource match([], []) -> true
			    rule 2.1: condition true -> true
			result: true
			""",
			"Request[requester=1, resource=[], from=2]"
		);
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				index(3)
			),
			"""
			evaluating Request[requester=1, resource=[], from=3]
			  policy 3: evaluating Request[requester=1, resource=[], from=3]
			    rule 3.1: resource match([], []) -> true
			    rule 3.1: condition true -> true
			result: true
			""",
			"Request[requester=1, resource=[], from=3]"
		);
	}

	@Test
	void shouldCheckRuleMatch() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice"),
				new Rules()
					.add(new Rule())))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper", "black")
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper", "white"),
						FALSE
					))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "David"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper", "white")
							.add("file/format", "PDF"),
						// the expression seems redundant,
						// but it checks that the requester asks
						// for both attributes
						new ExpressionWithDescription(
							c -> c.name("file/format").equals("PDF"),
							"file/format = PDF"
						)
					))))
		.add(
			new Policy( // index 5
				new Attributes()
					.add("name", "Edward"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper", "white"),
						new ExpressionWithDescription(
							c -> c.name("job/role").equals("writer"),
							"job/role = writer"
						)
					))))
		.add(
			new Policy( // index 6
				new Attributes()
					.add("name", "Faye"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper", "white"),
						new ExpressionWithDescription(
							c -> c.name("current/city").equals("Firenze"),
							"current/city = Firenze"
						)
					))));
		assertPolicies("""
		1 = Policy[party=[(name : Alice)], rules=[resource=[], condition=true]]
		2 = Policy[party=[(name : Bob)], rules=[resource=[(paper : black)], condition=true]]
		3 = Policy[party=[(name : Carl)], rules=[resource=[(paper : white)], condition=always false]]
		4 = Policy[party=[(name : David)], rules=[resource=[(paper : white), (file/format : PDF)], condition=file/format = PDF]]
		5 = Policy[party=[(name : Edward)], rules=[resource=[(paper : white)], condition=job/role = writer]]
		6 = Policy[party=[(name : Faye)], rules=[resource=[(paper : white)], condition=current/city = Firenze]]
		""");
		// Alice requests
		// ( resource: (paper : white), from: 2)
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white"),
				index(2)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white)], from=2]
			  policy 2: evaluating Request[requester=1, resource=[(paper : white)], from=2]
			    rule 2.1: resource match([(paper : white)], [(paper : black)]) -> false
			result: false
			"""
		);
		// Alice requests
		// ( resource: (paper : white), from: 3)
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white"),
				index(3)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white)], from=3]
			  policy 3: evaluating Request[requester=1, resource=[(paper : white)], from=3]
			    rule 3.1: resource match([(paper : white)], [(paper : white)]) -> true
			    rule 3.1: condition always false -> false
			result: false
			"""
		);
		// Alice requests
		// ( resource: (paper : white), from: 4)
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white"),
				index(4)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white)], from=4]
			  policy 4: evaluating Request[requester=1, resource=[(paper : white)], from=4]
			    rule 4.1: resource match([(paper : white)], [(paper : white), (file/format : PDF)]) -> true
			    rule 4.1: condition file/format = PDF -> Undefined name: file/format
			result: false
			"""
		);
		// Alice requests
		// ( resource: (paper : white)(file : PDF), from: 4)
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white")
					.add("file/format", "PDF"),
				index(4)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white), (file/format : PDF)], from=4]
			  policy 4: evaluating Request[requester=1, resource=[(paper : white), (file/format : PDF)], from=4]
			    rule 4.1: resource match([(paper : white), (file/format : PDF)], [(paper : white), (file/format : PDF)]) -> true
			    rule 4.1: condition file/format = PDF -> true
			result: true
			""",
			"Request[requester=1, resource=[(paper : white), (file/format : PDF)], from=4]"
		);
		// Alice requests
		// ( resource: (paper : white), from: 5)
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white"),
				index(5)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white)], from=5]
			  policy 5: evaluating Request[requester=1, resource=[(paper : white)], from=5]
			    rule 5.1: resource match([(paper : white)], [(paper : white)]) -> true
			    rule 5.1: condition job/role = writer -> Undefined name: job/role
			result: false
			"""
		);

		// TEST for ContextHandler

		semantics.contextHandler(new ContextHandler()
			.add(6, "current/city", "Firenze"));

		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white"),
				index(6)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white)], from=6]
			  policy 6: evaluating Request[requester=1, resource=[(paper : white)], from=6]
			    rule 6.1: resource match([(paper : white)], [(paper : white)]) -> true
			    rule 6.1: condition current/city = Firenze -> true
			result: true
			""",
			"Request[requester=1, resource=[(paper : white)], from=6]"
		);
	}

	@Test
	void shouldUseRequesterPartyAttributes() {
		// Carl only gives paper white to name Bob
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice"),
				new Rules()))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob"),
				new Rules()))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper", "white"),
						new ExpressionWithDescription(
								c -> c.name("name").equals("Bob"),
								"name = Bob"
							)
					))));
		// Alice requests
		// ( resource: (paper : white), from: 3)
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white"),
				index(3)
			),
			"""
			evaluating Request[requester=1, resource=[(paper : white)], from=3]
			  policy 3: evaluating Request[requester=1, resource=[(paper : white)], from=3]
			    rule 3.1: resource match([(paper : white)], [(paper : white)]) -> true
			    rule 3.1: condition name = Bob -> false
			result: false
			"""
		);
		// Bob requests
		// ( resource: (paper : white), from: 3)
		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("paper", "white"),
				index(3)
			),
			"""
			evaluating Request[requester=2, resource=[(paper : white)], from=3]
			  policy 3: evaluating Request[requester=2, resource=[(paper : white)], from=3]
			    rule 3.1: resource match([(paper : white)], [(paper : white)]) -> true
			    rule 3.1: condition name = Bob -> true
			result: true
			""",
			"Request[requester=2, resource=[(paper : white)], from=3]"
		);
	}

	@Test
	void shouldCheckAnyAndAllSuchThat() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice"),
				new Rules()
					.add(new Rule())))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("role", "Provider"),
				new Rules()
					.add(new Rule())))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Bob")
					.add("role", "Provider"),
				new Rules()
					.add(new Rule())));
		assertPolicies("""
		1 = Policy[party=[(name : Alice)], rules=[resource=[], condition=true]]
		2 = Policy[party=[(role : Provider)], rules=[resource=[], condition=true]]
		3 = Policy[party=[(name : Bob), (role : Provider)], rules=[resource=[], condition=true]]
			""");
		// Alice requests
		// ( resource: empty, from: anySuchThat (name : "Bob"))
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				anySuchThat(new Attributes()
					.add("name", "Bob"))
			),
			"""
			evaluating Request[requester=1, resource=[], from=anySuchThat: [(name : Bob)]]
			  finding matching policies
			    policy 2: from match([(name : Bob)], [(role : Provider)]) -> false
			    policy 3: from match([(name : Bob)], [(name : Bob), (role : Provider)]) -> true
			  policy 3: evaluating Request[requester=1, resource=[], from=3]
			    rule 3.1: resource match([], []) -> true
			    rule 3.1: condition true -> true
			result: true
			""",
			"Request[requester=1, resource=[], from=3]"
		);
		// Alice requests
		// ( resource: empty, from: allSuchThat (role : "Provider"))
		// NOTE: Alice's policy is not evaluated since she's the requester
		// her attributes would not match the request
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				allSuchThat(new Attributes()
					.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[], from=allSuchThat: [(role : Provider)]]
			  finding matching policies
			    policy 2: from match([(role : Provider)], [(role : Provider)]) -> true
			    policy 3: from match([(role : Provider)], [(name : Bob), (role : Provider)]) -> true
			  policy 2: evaluating Request[requester=1, resource=[], from=2]
			    rule 2.1: resource match([], []) -> true
			    rule 2.1: condition true -> true
			  policy 3: evaluating Request[requester=1, resource=[], from=3]
			    rule 3.1: resource match([], []) -> true
			    rule 3.1: condition true -> true
			result: true
			""",
			"""
			Request[requester=1, resource=[], from=2]
			Request[requester=1, resource=[], from=3]"""
		);
		// Alice requests
		// ( resource: empty, from: allSuchThat (name : "Bob"))
		// even if there's only one Bob
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				allSuchThat(new Attributes()
					.add("name", "Bob"))
			),
			"""
			evaluating Request[requester=1, resource=[], from=allSuchThat: [(name : Bob)]]
			  finding matching policies
			    policy 2: from match([(name : Bob)], [(role : Provider)]) -> false
			    policy 3: from match([(name : Bob)], [(name : Bob), (role : Provider)]) -> true
			  policy 3: evaluating Request[requester=1, resource=[], from=3]
			    rule 3.1: resource match([], []) -> true
			    rule 3.1: condition true -> true
			result: true
			""",
			"Request[requester=1, resource=[], from=3]"
		);
		// Alice requests
		// ( resource: empty, from: allSuchThat (name : "Carl"))
		// and there's no Carl
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes(),
				allSuchThat(new Attributes()
					.add("name", "Carl"))
			),
			"""
			evaluating Request[requester=1, resource=[], from=allSuchThat: [(name : Carl)]]
			  finding matching policies
			    policy 2: from match([(name : Carl)], [(role : Provider)]) -> false
			    policy 3: from match([(name : Carl)], [(name : Bob), (role : Provider)]) -> false
			result: false
			"""
		);
	}

	@Test
	void simpleExchange() {
		// Alice gives printer provided the requester gives paper
		// Bob gives paper
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("resource/type", "paper"),
							requester())))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice), (role : PrinterProvider)], rules=[resource=[(resource/type : printer)], condition=true, exchange=Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER]]]
			2 = Policy[party=[(name : Bob), (role : PaperProvider)], rules=[resource=[(resource/type : paper)], condition=true]]
			""");
		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			      policy 2: evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			        rule 2.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 2.1: condition true -> true
			    result: true
			result: true
			""",
			"""
			Request[requester=2, resource=[(resource/type : printer)], from=1]
			Request[requester=1, resource=[(resource/type : paper)], from=2]"""
		);
	}

	@Test
	void mutualExchange() {
		// Alice gives printer provided the requester gives paper
		// Bob gives paper provided the requester gives ink
		// Carl gives paper provided the requester gives printer
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("resource/type", "paper"),
							requester())))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper"),
						new SingleExchange(
								me(),
								new Attributes()
									.add("resource/type", "ink"),
								requester())))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper"),
						new SingleExchange(
								me(),
								new Attributes()
									.add("resource/type", "printer"),
								requester())))));
		assertPolicies("""
		1 = Policy[party=[(name : Alice), (role : PrinterProvider)], rules=[resource=[(resource/type : printer)], condition=true, exchange=Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER]]]
		2 = Policy[party=[(name : Bob), (role : PaperProvider)], rules=[resource=[(resource/type : paper)], condition=true, exchange=Exchange[to=ME, resource=[(resource/type : ink)], from=REQUESTER]]]
		3 = Policy[party=[(name : Carl), (role : PaperProvider)], rules=[resource=[(resource/type : paper)], condition=true, exchange=Exchange[to=ME, resource=[(resource/type : printer)], from=REQUESTER]]]
			""");
		assertResultFalse(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			      policy 2: evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			        rule 2.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 2.1: condition true -> true
			        rule 2.1: evaluating Exchange[to=ME, resource=[(resource/type : ink)], from=REQUESTER]
			        evaluating Request[requester=2, resource=[(resource/type : ink)], from=1]
			          policy 1: evaluating Request[requester=2, resource=[(resource/type : ink)], from=1]
			            rule 1.1: resource match([(resource/type : ink)], [(resource/type : printer)]) -> false
			        result: false
			    result: false
			result: false
			"""
		);
		assertResultTrue(
			new Request(
				index(3), // Carl
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=3, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			  policy 1: evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			      policy 3: evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			        rule 3.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 3.1: condition true -> true
			        rule 3.1: evaluating Exchange[to=ME, resource=[(resource/type : printer)], from=REQUESTER]
			        rule 3.1: compliant request found Request[requester=3, resource=[(resource/type : printer)], from=1]
			    result: true
			result: true
			""",
			"""
			Request[requester=3, resource=[(resource/type : printer)], from=1]
			Request[requester=1, resource=[(resource/type : paper)], from=3]"""
		);
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/type", "paper"),
				anySuchThat(new Attributes()
					.add("role", "PaperProvider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/type : paper)], from=anySuchThat: [(role : PaperProvider)]]
			  finding matching policies
			    policy 2: from match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider)]) -> true
			    policy 3: from match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider)]) -> true
			  policy 2: evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			    rule 2.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			    rule 2.1: condition true -> true
			    rule 2.1: evaluating Exchange[to=ME, resource=[(resource/type : ink)], from=REQUESTER]
			    evaluating Request[requester=2, resource=[(resource/type : ink)], from=1]
			      policy 1: evaluating Request[requester=2, resource=[(resource/type : ink)], from=1]
			        rule 1.1: resource match([(resource/type : ink)], [(resource/type : printer)]) -> false
			    result: false
			  policy 3: evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			    rule 3.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			    rule 3.1: condition true -> true
			    rule 3.1: evaluating Exchange[to=ME, resource=[(resource/type : printer)], from=REQUESTER]
			    evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			      policy 1: evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			        rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			        rule 1.1: condition true -> true
			        rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER]
			        rule 1.1: compliant request found Request[requester=1, resource=[(resource/type : paper)], from=3]
			    result: true
			result: true
			""",
			"""
			Request[requester=1, resource=[(resource/type : paper)], from=3]
			Request[requester=3, resource=[(resource/type : printer)], from=1]"""
		);
	}

	@Test
	void mutualExchangeWithCustomRequestComply() {
		// the custom comply predicate just checks that the requester and from are the
		// same in the existing requests
		// Alice gives printer provided the requester gives paper to me
		// Bob gives paper provided the requester gives ink
		// Carl gives paper provided the requester gives printer
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("resource/type", "paper"),
							requester())))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper"),
						new SingleExchange(
								me(),
								new Attributes()
									.add("resource/type", "ink"),
								requester())))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper"),
						new SingleExchange(
								me(),
								new Attributes()
									.add("resource/type", "printer"),
								requester())))));

		// as long as a request is found with the same requester and from
		// then we consider the exchange succeeds
		semantics.requestComply((newRequest, existingRequest) ->
			newRequest.requester().equals(existingRequest.requester())
			&& newRequest.from().equals(existingRequest.from())
		);

		// though Bob provides ink instead of paper, the custom compliant predicate
		// above makes the exchange succeed
		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			      policy 2: evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			        rule 2.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 2.1: condition true -> true
			        rule 2.1: evaluating Exchange[to=ME, resource=[(resource/type : ink)], from=REQUESTER]
			        rule 2.1: compliant request found Request[requester=2, resource=[(resource/type : ink)], from=1]
			    result: true
			result: true
			""",
			"""
			Request[requester=2, resource=[(resource/type : printer)], from=1]
			Request[requester=1, resource=[(resource/type : paper)], from=2]"""
		);
		assertResultTrue(
			new Request(
				index(3), // Carl
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=3, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			  policy 1: evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			      policy 3: evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			        rule 3.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 3.1: condition true -> true
			        rule 3.1: evaluating Exchange[to=ME, resource=[(resource/type : printer)], from=REQUESTER]
			        rule 3.1: compliant request found Request[requester=3, resource=[(resource/type : printer)], from=1]
			    result: true
			result: true
			""",
			"""
			Request[requester=3, resource=[(resource/type : printer)], from=1]
			Request[requester=1, resource=[(resource/type : paper)], from=3]"""
		);
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/type", "paper"),
				anySuchThat(new Attributes()
					.add("role", "PaperProvider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/type : paper)], from=anySuchThat: [(role : PaperProvider)]]
			  finding matching policies
			    policy 2: from match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider)]) -> true
			    policy 3: from match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider)]) -> true
			  policy 2: evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			    rule 2.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			    rule 2.1: condition true -> true
			    rule 2.1: evaluating Exchange[to=ME, resource=[(resource/type : ink)], from=REQUESTER]
			    evaluating Request[requester=2, resource=[(resource/type : ink)], from=1]
			      policy 1: evaluating Request[requester=2, resource=[(resource/type : ink)], from=1]
			        rule 1.1: resource match([(resource/type : ink)], [(resource/type : printer)]) -> false
			    result: false
			  policy 3: evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			    rule 3.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			    rule 3.1: condition true -> true
			    rule 3.1: evaluating Exchange[to=ME, resource=[(resource/type : printer)], from=REQUESTER]
			    evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			      policy 1: evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			        rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			        rule 1.1: condition true -> true
			        rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER]
			        rule 1.1: compliant request found Request[requester=1, resource=[(resource/type : paper)], from=3]
			    result: true
			result: true
			""",
			"""
			Request[requester=1, resource=[(resource/type : paper)], from=3]
			Request[requester=3, resource=[(resource/type : printer)], from=1]"""
		);
	}

	@Test
	void orExchange() {
		// Alice gives printer provided the requester gives white paper or yellow paper
		// Bob gives white paper
		// Carl gives yellow paper
		// Ed gives green paper
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new OrExchange(
							new SingleExchange(
								me(),
								new Attributes()
									.add("paper/color", "white"),
								requester()),
							new SingleExchange(
								me(),
								new Attributes()
									.add("paper/color", "yellow"),
								requester())
						)))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper/color", "white")
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper/color", "yellow")
					))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "Ed")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper/color", "green")
					))));
		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PaperProvider)]) -> false
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating OR(Exchange[to=ME, resource=[(paper/color : white)], from=REQUESTER], Exchange[to=ME, resource=[(paper/color : yellow)], from=REQUESTER])
			      rule 1.1: evaluating Exchange[to=ME, resource=[(paper/color : white)], from=REQUESTER]
			      evaluating Request[requester=1, resource=[(paper/color : white)], from=2]
			        policy 2: evaluating Request[requester=1, resource=[(paper/color : white)], from=2]
			          rule 2.1: resource match([(paper/color : white)], [(paper/color : white)]) -> true
			          rule 2.1: condition true -> true
			      result: true
			    rule 1.1: END Exchange -> true
			result: true
			""",
			"""
			Request[requester=2, resource=[(resource/type : printer)], from=1]
			Request[requester=1, resource=[(paper/color : white)], from=2]"""
		);
		assertResultTrue(
			new Request(
				index(3), // Carl
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=3, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PaperProvider)]) -> false
			  policy 1: evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating OR(Exchange[to=ME, resource=[(paper/color : white)], from=REQUESTER], Exchange[to=ME, resource=[(paper/color : yellow)], from=REQUESTER])
			      rule 1.1: evaluating Exchange[to=ME, resource=[(paper/color : white)], from=REQUESTER]
			      evaluating Request[requester=1, resource=[(paper/color : white)], from=3]
			        policy 3: evaluating Request[requester=1, resource=[(paper/color : white)], from=3]
			          rule 3.1: resource match([(paper/color : white)], [(paper/color : yellow)]) -> false
			      result: false
			    rule 1.1: OR
			      rule 1.1: evaluating Exchange[to=ME, resource=[(paper/color : yellow)], from=REQUESTER]
			      evaluating Request[requester=1, resource=[(paper/color : yellow)], from=3]
			        policy 3: evaluating Request[requester=1, resource=[(paper/color : yellow)], from=3]
			          rule 3.1: resource match([(paper/color : yellow)], [(paper/color : yellow)]) -> true
			          rule 3.1: condition true -> true
			      result: true
			    rule 1.1: END Exchange -> true
			result: true
			""",
			"""
			Request[requester=3, resource=[(resource/type : printer)], from=1]
			Request[requester=1, resource=[(paper/color : yellow)], from=3]"""
		);
		assertResultFalse(
			new Request(
				index(4), // Ed
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=4, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			  policy 1: evaluating Request[requester=4, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating OR(Exchange[to=ME, resource=[(paper/color : white)], from=REQUESTER], Exchange[to=ME, resource=[(paper/color : yellow)], from=REQUESTER])
			      rule 1.1: evaluating Exchange[to=ME, resource=[(paper/color : white)], from=REQUESTER]
			      evaluating Request[requester=1, resource=[(paper/color : white)], from=4]
			        policy 4: evaluating Request[requester=1, resource=[(paper/color : white)], from=4]
			          rule 4.1: resource match([(paper/color : white)], [(paper/color : green)]) -> false
			      result: false
			    rule 1.1: OR
			      rule 1.1: evaluating Exchange[to=ME, resource=[(paper/color : yellow)], from=REQUESTER]
			      evaluating Request[requester=1, resource=[(paper/color : yellow)], from=4]
			        policy 4: evaluating Request[requester=1, resource=[(paper/color : yellow)], from=4]
			          rule 4.1: resource match([(paper/color : yellow)], [(paper/color : green)]) -> false
			      result: false
			    rule 1.1: END Exchange -> false
			result: false
			"""
		);
	}

	@Test
	void andExchange() {
		// Alice gives printer provided the requester gives paper and color white
		// Bob gives paper and color white
		// Carl gives paper
		// Ed gives color white
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new AndExchange(
							new SingleExchange(
								me(),
								new Attributes()
									.add("resource/type", "paper"),
								requester()),
							new SingleExchange(
								me(),
								new Attributes()
									.add("color", "white"),
								requester())
						)))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
							.add("color", "white")
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "Ed")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("color", "white")
					))));
		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PaperProvider)]) -> false
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating AND(Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER], Exchange[to=ME, resource=[(color : white)], from=REQUESTER])
			      rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER]
			      evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			        policy 2: evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			          rule 2.1: resource match([(resource/type : paper)], [(resource/type : paper), (color : white)]) -> true
			          rule 2.1: condition true -> true
			      result: true
			    rule 1.1: AND
			      rule 1.1: evaluating Exchange[to=ME, resource=[(color : white)], from=REQUESTER]
			      evaluating Request[requester=1, resource=[(color : white)], from=2]
			        policy 2: evaluating Request[requester=1, resource=[(color : white)], from=2]
			          rule 2.1: resource match([(color : white)], [(resource/type : paper), (color : white)]) -> true
			          rule 2.1: condition true -> true
			      result: true
			    rule 1.1: END Exchange -> true
			result: true
			""",
			"""
			Request[requester=2, resource=[(resource/type : printer)], from=1]
			Request[requester=1, resource=[(resource/type : paper)], from=2]
			Request[requester=1, resource=[(color : white)], from=2]"""
		);
		assertResultFalse(
			new Request(
				index(3), // Carl
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=3, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PaperProvider)]) -> false
			  policy 1: evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating AND(Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER], Exchange[to=ME, resource=[(color : white)], from=REQUESTER])
			      rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER]
			      evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			        policy 3: evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			          rule 3.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			          rule 3.1: condition true -> true
			      result: true
			    rule 1.1: AND
			      rule 1.1: evaluating Exchange[to=ME, resource=[(color : white)], from=REQUESTER]
			      evaluating Request[requester=1, resource=[(color : white)], from=3]
			        policy 3: evaluating Request[requester=1, resource=[(color : white)], from=3]
			          rule 3.1: resource match([(color : white)], [(resource/type : paper)]) -> false
			      result: false
			    rule 1.1: END Exchange -> false
			result: false
			"""
		);
		assertResultFalse(
			new Request(
				index(4), // Ed
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=4, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			  policy 1: evaluating Request[requester=4, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating AND(Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER], Exchange[to=ME, resource=[(color : white)], from=REQUESTER])
			      rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=REQUESTER]
			      evaluating Request[requester=1, resource=[(resource/type : paper)], from=4]
			        policy 4: evaluating Request[requester=1, resource=[(resource/type : paper)], from=4]
			          rule 4.1: resource match([(resource/type : paper)], [(color : white)]) -> false
			      result: false
			    rule 1.1: END Exchange -> false
			result: false
			"""
		);
	}

	@Test
	void exchangeFromAnySuchThat() {
		// Alice gives printer provided any paper provider gives green paper
		// Bob gives white paper
		// Carl gives yellow paper
		// Ed gives green paper
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("paper/color", "green"),
							anySuchThat(new Attributes()
									.add("role", "PaperProvider")))
						))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper/color", "white")
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper/color", "yellow")
					))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "Ed")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("paper/color", "green")
					))));
		assertPolicies("""
		1 = Policy[party=[(name : Alice), (role : PrinterProvider)], rules=[resource=[(resource/type : printer)], condition=true, exchange=Exchange[to=ME, resource=[(paper/color : green)], from=anySuchThat: [(role : PaperProvider)]]]]
		2 = Policy[party=[(name : Bob), (role : PaperProvider)], rules=[resource=[(paper/color : white)], condition=true]]
		3 = Policy[party=[(name : Carl), (role : PaperProvider)], rules=[resource=[(paper/color : yellow)], condition=true]]
		4 = Policy[party=[(name : Ed), (role : PaperProvider)], rules=[resource=[(paper/color : green)], condition=true]]
			""");
		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PaperProvider)]) -> false
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=ME, resource=[(paper/color : green)], from=anySuchThat: [(role : PaperProvider)]]
			    policy 1: from match([(role : PaperProvider)], [(name : Alice), (role : PrinterProvider)]) -> false
			    policy 2: from match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider)]) -> true
			    policy 3: from match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider)]) -> true
			    policy 4: from match([(role : PaperProvider)], [(name : Ed), (role : PaperProvider)]) -> true
			    evaluating Request[requester=1, resource=[(paper/color : green)], from=2]
			      policy 2: evaluating Request[requester=1, resource=[(paper/color : green)], from=2]
			        rule 2.1: resource match([(paper/color : green)], [(paper/color : white)]) -> false
			    result: false
			    evaluating Request[requester=1, resource=[(paper/color : green)], from=3]
			      policy 3: evaluating Request[requester=1, resource=[(paper/color : green)], from=3]
			        rule 3.1: resource match([(paper/color : green)], [(paper/color : yellow)]) -> false
			    result: false
			    evaluating Request[requester=1, resource=[(paper/color : green)], from=4]
			      policy 4: evaluating Request[requester=1, resource=[(paper/color : green)], from=4]
			        rule 4.1: resource match([(paper/color : green)], [(paper/color : green)]) -> true
			        rule 4.1: condition true -> true
			    result: true
			result: true
			""",
			"""
			Request[requester=2, resource=[(resource/type : printer)], from=1]
			Request[requester=1, resource=[(paper/color : green)], from=4]"""
		);
	}

	@Test
	void exchangeFromAllSuchThat() {
		// Alice gives printer provided any paper provider gives paper
		// Bob gives paper
		// Carl gives paper
		// Ed gives paper
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("resource/type", "paper"),
							allSuchThat(new Attributes()
									.add("role", "PaperProvider")))
						))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "Ed")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))));
		assertPolicies("""
		1 = Policy[party=[(name : Alice), (role : PrinterProvider)], rules=[resource=[(resource/type : printer)], condition=true, exchange=Exchange[to=ME, resource=[(resource/type : paper)], from=allSuchThat: [(role : PaperProvider)]]]]
		2 = Policy[party=[(name : Bob), (role : PaperProvider)], rules=[resource=[(resource/type : paper)], condition=true]]
		3 = Policy[party=[(name : Carl), (role : PaperProvider)], rules=[resource=[(resource/type : paper)], condition=true]]
		4 = Policy[party=[(name : Ed), (role : PaperProvider)], rules=[resource=[(resource/type : paper)], condition=true]]
			""");
		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PaperProvider)]) -> false
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=allSuchThat: [(role : PaperProvider)]]
			    policy 1: from match([(role : PaperProvider)], [(name : Alice), (role : PrinterProvider)]) -> false
			    policy 2: from match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider)]) -> true
			    policy 3: from match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider)]) -> true
			    policy 4: from match([(role : PaperProvider)], [(name : Ed), (role : PaperProvider)]) -> true
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			      policy 2: evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			        rule 2.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 2.1: condition true -> true
			    result: true
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			      policy 3: evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			        rule 3.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 3.1: condition true -> true
			    result: true
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=4]
			      policy 4: evaluating Request[requester=1, resource=[(resource/type : paper)], from=4]
			        rule 4.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 4.1: condition true -> true
			    result: true
			result: true
			""",
			"""
			Request[requester=2, resource=[(resource/type : printer)], from=1]
			Request[requester=1, resource=[(resource/type : paper)], from=2]
			Request[requester=1, resource=[(resource/type : paper)], from=3]
			Request[requester=1, resource=[(resource/type : paper)], from=4]"""
		);
	}

	@Test
	void exchangeToAllSuchThat() {
		// Alice gives printer provided the requester gives paper to all printer providers
		// Bob gives paper
		// Carl gives paper
		// Ed is a printer provider
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							allSuchThat(new Attributes()
									.add("role", "PrinterProvider")),
							new Attributes()
								.add("resource/type", "paper"),
							requester())
						))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "Ed")
					.add("role", "PrinterProvider"),
				new Rules()));

		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PrinterProvider)]) -> true
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=allSuchThat: [(role : PrinterProvider)], resource=[(resource/type : paper)], from=REQUESTER]
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PrinterProvider)]) -> true
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			      policy 2: evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			        rule 2.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 2.1: condition true -> true
			    result: true
			    evaluating Request[requester=4, resource=[(resource/type : paper)], from=2]
			      policy 2: evaluating Request[requester=4, resource=[(resource/type : paper)], from=2]
			        rule 2.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 2.1: condition true -> true
			    result: true
			result: true
			""",
			"""
			Request[requester=2, resource=[(resource/type : printer)], from=1]
			Request[requester=1, resource=[(resource/type : paper)], from=2]
			Request[requester=4, resource=[(resource/type : paper)], from=2]"""
		);
	}

	@Test
	void exchangeToAnySuchThat() {
		// Alice gives printer provided the requester gives paper to any printer providers
		// Bob gives paper
		// Carl gives paper
		// Ed is a printer provider
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							anySuchThat(new Attributes()
									.add("role", "PrinterProvider")),
							new Attributes()
								.add("resource/type", "paper"),
							requester())
						))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "Ed")
					.add("role", "PrinterProvider"),
				new Rules()));

		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PrinterProvider)]) -> true
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=anySuchThat: [(role : PrinterProvider)], resource=[(resource/type : paper)], from=REQUESTER]
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PrinterProvider)]) -> true
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			      policy 2: evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			        rule 2.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 2.1: condition true -> true
			    result: true
			result: true
			""",
			"""
			Request[requester=2, resource=[(resource/type : printer)], from=1]
			Request[requester=1, resource=[(resource/type : paper)], from=2]"""
		);
	}

	@Test
	void exchangeToIsEmptyEvaluatesToTrue() {
		// Alice gives printer provided the requester gives paper to any Ink providers
		// Bob gives paper
		// there's no InkProvider
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							anySuchThat(new Attributes()
									.add("role", "InkProvider")),
							new Attributes()
								.add("resource/type", "paper"),
							requester())
						))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))));

		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=anySuchThat: [(role : InkProvider)], resource=[(resource/type : paper)], from=REQUESTER]
			    policy 1: from match([(role : InkProvider)], [(name : Alice), (role : PrinterProvider)]) -> false
			    policy 2: from match([(role : InkProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    rule 1.1: satisfied: no one to exchange
			result: true
			""",
			"Request[requester=2, resource=[(resource/type : printer)], from=1]"
		);
	}

	@Test
	void exchangeFromIsEmptyEvaluatesToFalse() {
		// Alice gives printer provided any Ink providers gives ink to me
		// Bob gives paper
		// there's no InkProvider
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("resource/type", "paper"),
							anySuchThat(new Attributes()
									.add("role", "InkProvider")))
						))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))));

		assertResultFalse(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=anySuchThat: [(role : InkProvider)]]
			    policy 1: from match([(role : InkProvider)], [(name : Alice), (role : PrinterProvider)]) -> false
			    policy 2: from match([(role : InkProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    rule 1.1: not satisfied: no one from exchange
			result: false
			"""
		);
	}

	@Test
	void exchangeFromAndToWouldBeEqual() {
		// Alice gives printer provided any printer provider gives paper to me
		// Bob gives paper
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							me(),
							new Attributes()
								.add("resource/type", "paper"),
							anySuchThat(new Attributes()
								.add("role", "PrinterProvider")))
						))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))));

		assertResultFalse(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=ME, resource=[(resource/type : paper)], from=anySuchThat: [(role : PrinterProvider)]]
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			result: false
			"""
		);
	}

	@Test
	void exchangeFromAndToWouldBeEqualWithAllSuchThat() {
		// Alice gives printer provided all printer provider gives paper to all printer provider
		// Bob gives paper
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							allSuchThat(new Attributes()
									.add("role", "PrinterProvider")),
							new Attributes()
								.add("resource/type", "paper"),
							allSuchThat(new Attributes()
								.add("role", "PrinterProvider")))
						))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))));

		assertResultFalse(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=allSuchThat: [(role : PrinterProvider)], resource=[(resource/type : paper)], from=allSuchThat: [(role : PrinterProvider)]]
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    rule 1.1: not satisfied: no request could be generated
			result: false
			"""
		);
	}

	@Test
	void exchangeFromAndToWouldBeEqualWithAnySuchThat() {
		// Alice gives printer provided any printer provider gives paper to any printer provider
		// Bob gives paper
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							anySuchThat(new Attributes()
									.add("role", "PrinterProvider")),
							new Attributes()
								.add("resource/type", "paper"),
							anySuchThat(new Attributes()
								.add("role", "PrinterProvider")))
						))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))));

		assertResultFalse(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=anySuchThat: [(role : PrinterProvider)], resource=[(resource/type : paper)], from=anySuchThat: [(role : PrinterProvider)]]
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			result: false
			"""
		);
	}

	@Test
	void exchangeFromToAllSuchThat() {
		// Alice gives printer provided the all paper provider gives paper to all printer providers
		// Bob gives paper
		// Carl gives paper
		// Ed is a printer provider
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "printer"),
						new SingleExchange(
							allSuchThat(new Attributes()
									.add("role", "PrinterProvider")),
							new Attributes()
								.add("resource/type", "paper"),
							allSuchThat(new Attributes()
									.add("role", "PaperProvider")))
						))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl")
					.add("role", "PaperProvider"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("resource/type", "paper")
					))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "Ed")
					.add("role", "PrinterProvider"),
				new Rules()));

		assertResultTrue(
			new Request(
				index(2), // Bob
				new Attributes()
					.add("resource/type", "printer"),
				anySuchThat(new Attributes()
					.add("role", "PrinterProvider"))
			),
			"""
			evaluating Request[requester=2, resource=[(resource/type : printer)], from=anySuchThat: [(role : PrinterProvider)]]
			  finding matching policies
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PrinterProvider)]) -> true
			  policy 1: evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    rule 1.1: resource match([(resource/type : printer)], [(resource/type : printer)]) -> true
			    rule 1.1: condition true -> true
			    rule 1.1: evaluating Exchange[to=allSuchThat: [(role : PrinterProvider)], resource=[(resource/type : paper)], from=allSuchThat: [(role : PaperProvider)]]
			    policy 1: from match([(role : PaperProvider)], [(name : Alice), (role : PrinterProvider)]) -> false
			    policy 2: from match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider)]) -> true
			    policy 3: from match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider)]) -> true
			    policy 4: from match([(role : PaperProvider)], [(name : Ed), (role : PrinterProvider)]) -> false
			    policy 1: from match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider)]) -> true
			    policy 2: from match([(role : PrinterProvider)], [(name : Bob), (role : PaperProvider)]) -> false
			    policy 3: from match([(role : PrinterProvider)], [(name : Carl), (role : PaperProvider)]) -> false
			    policy 4: from match([(role : PrinterProvider)], [(name : Ed), (role : PrinterProvider)]) -> true
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			      policy 2: evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			        rule 2.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 2.1: condition true -> true
			    result: true
			    evaluating Request[requester=4, resource=[(resource/type : paper)], from=2]
			      policy 2: evaluating Request[requester=4, resource=[(resource/type : paper)], from=2]
			        rule 2.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 2.1: condition true -> true
			    result: true
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			      policy 3: evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			        rule 3.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 3.1: condition true -> true
			    result: true
			    evaluating Request[requester=4, resource=[(resource/type : paper)], from=3]
			      policy 3: evaluating Request[requester=4, resource=[(resource/type : paper)], from=3]
			        rule 3.1: resource match([(resource/type : paper)], [(resource/type : paper)]) -> true
			        rule 3.1: condition true -> true
			    result: true
			result: true
			""",
			"""
			Request[requester=2, resource=[(resource/type : printer)], from=1]
			Request[requester=1, resource=[(resource/type : paper)], from=2]
			Request[requester=4, resource=[(resource/type : paper)], from=2]
			Request[requester=1, resource=[(resource/type : paper)], from=3]
			Request[requester=4, resource=[(resource/type : paper)], from=3]"""
		);
	}

	private void assertPolicies(String expected) {
		assertEquals(expected, policies.description());
	}

	private void assertResultTrue(Request request, String expectedTrace, String expectedRequests) {
		var result = semantics.evaluate(request);
		assertAll(
			() -> assertTrue(result.isPermitted()),
			() -> assertEquals(expectedTrace, semantics.getTrace().toString()),
			() -> assertEquals(expectedRequests,
				result.getRequests().stream().map(Object::toString).collect(Collectors.joining("\n")))
		);
	}

	private void assertResultFalse(Request request, String expectedTrace) {
		assertAll(
			() -> assertFalse(semantics.evaluate(request).isPermitted()),
			() -> assertEquals(expectedTrace, semantics.getTrace().toString())
		);
	}
}
