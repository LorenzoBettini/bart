package colpo.core.semantics;

import static colpo.core.Participant.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import colpo.core.Attributes;
import colpo.core.Exchange;
import colpo.core.ExpressionWithDescription;
import colpo.core.Policies;
import colpo.core.Policy;
import colpo.core.Request;
import colpo.core.Rule;
import colpo.core.Rules;

class SemanticsTest {

	private Semantics semantics;
	private Policies policies;

	private static final ExpressionWithDescription TRUE =
			new ExpressionWithDescription(context -> true, "true");
	private static final ExpressionWithDescription FALSE =
			new ExpressionWithDescription(context -> false, "false");

	@BeforeEach
	void init() {
		policies = new Policies();
		semantics = new Semantics(policies);
	}

	@Test
	void shouldCheckRequestFrom() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice"),
				new Rules()
					.add(new Rule(FALSE))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob"),
				new Rules()
					.add(new Rule(TRUE))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Carl"),
				new Rules()
					.add(new Rule(TRUE))));
		assertPolicies("""
		1 = Policy[party=[(name : Alice)], rules=[resource=false]]
		2 = Policy[party=[(name : Bob)], rules=[resource=true]]
		3 = Policy[party=[(name : Carl)], rules=[resource=true]]
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
			  2: expression true -> true
			result: true
			"""
		);
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				index(3)
			),
			"""
			evaluating Request[requester=1, resource=[], from=3]
			  3: expression true -> true
			result: true
			"""
		);
		// a requester should not refer to him/herself
		// this is just an internal test to check correct use of from
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes(),
				index(1)
			),
			"""
			evaluating Request[requester=1, resource=[], from=1]
			  1: expression false -> false
			result: false
			"""
		);
	}

	// TODO: finish after trace update
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
							.add("paper", "white"),
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
					))));
		assertPolicies("""
		1 = Policy[party=[(name : Alice)], rules=[resource=[], condition=true]]
		2 = Policy[party=[(name : Bob)], rules=[resource=[(paper : black)], condition=true]]
		3 = Policy[party=[(name : Carl)], rules=[resource=[(paper : white)], condition=always false]]
		4 = Policy[party=[(name : David)], rules=[resource=[(paper : white)], condition=file/format = PDF]]
		5 = Policy[party=[(name : Edward)], rules=[resource=[(paper : white)], condition=job/role = writer]]
		""");
		// Alice requests
		// ( resource: (paper : white), from: 2)
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("paper", "white"),
				new Attributes(),
				index(2)
			),
			"""
			evaluating Request[requester=1, resource=[], credentials=[], from=2]
			  2: condition true -> true
			result: true
			"""
		);
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes(),
				new Attributes(),
				index(3)
			),
			"""
			evaluating Request[requester=1, resource=[], credentials=[], from=3]
			  3: condition true -> true
			result: true
			"""
		);
	}

	// TODO: change it: this does not check attributes
	// it only checks whether parties match
	@Test
	void shouldCheckAttributesMatch() {
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
		// ( resource: (resource/name : "aResource"), from: anySuchThat (name : "Bob"))
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new Attributes(),
				anySuchThat(new Attributes()
					.add("name", "Bob"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], credentials=[], from=anySuchThat: [(name : Bob)]]
			  finding matching policies
			    2: false match([(name : Bob)], [(role : Provider)])
			    3: true match([(name : Bob)], [(name : Bob), (role : Provider)])
			  3: condition true -> true
			result: true
			"""
		);
		// Alice requests
		// ( resource: (resource/name : "aResource"), from: allSuchThat (role : "Provider"))
		// NOTE: Alice's policy is not evaluated since she's the requester
		// her attributes would not match the request
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new Attributes(),
				allSuchThat(new Attributes()
					.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], credentials=[], from=allSuchThat: [(role : Provider)]]
			  finding matching policies
			    2: true match([(role : Provider)], [(role : Provider)])
			    3: true match([(role : Provider)], [(name : Bob), (role : Provider)])
			  2: condition true -> true
			  3: condition true -> true
			result: true
			"""
		);
		// Alice requests
		// ( resource: (resource/name : "aResource"), from: allSuchThat (name : "Bob"))
		// even if there's only one Bob
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new Attributes(),
				allSuchThat(new Attributes()
					.add("name", "Bob"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], credentials=[], from=allSuchThat: [(name : Bob)]]
			  finding matching policies
			    2: false match([(name : Bob)], [(role : Provider)])
			    3: true match([(name : Bob)], [(name : Bob), (role : Provider)])
			  3: condition true -> true
			result: true
			"""
		);
		// Alice requests
		// ( resource: (resource/name : "aResource"), from: allSuchThat (name : "Carl"))
		// and there's no Carl
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource", "aResource"),
				new Attributes(),
				allSuchThat(new Attributes()
					.add("name", "Carl"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource : aResource)], credentials=[], from=allSuchThat: [(name : Carl)]]
			  finding matching policies
			    2: false match([(name : Carl)], [(role : Provider)])
			    3: false match([(name : Carl)], [(name : Bob), (role : Provider)])
			result: false
			"""
		);
	}

	@Test
	void shouldCheckExpression() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice"),
				new Rules()
					.add(new Rule(FALSE))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("role", "Provider")
					.add("resource/name", "aResource"),
				new Rules()
					.add(new Rule(
						new ExpressionWithDescription(
							c -> "read".equals(c.attribute("resource/usage")),
							"resource/usage = read")
						))))
		.add(
			new Policy( // index 3
				new Attributes()
					.add("name", "Bob")
					.add("role", "Provider")
					.add("resource/name", "aResource"),
				new Rules()
					.add(new Rule(
						new ExpressionWithDescription(
							c -> "read".equals(c.attribute("resource/usage")) ||
									"write".equals(c.attribute("resource/usage")),
							"resource/usage = read or resource/usage = write")
						))))
		.add(
			new Policy( // index 4
				new Attributes()
					.add("name", "Carl")
					.add("role", "SpecialProvider")
					.add("resource/name", "aResource"),
				new Rules()
					.add(new Rule(
						new ExpressionWithDescription(
							c -> "read".equals(c.attribute("resource/access")),
							"resource/access = read")
						))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice)], rules=[resource=false]]
			2 = Policy[party=[(role : Provider), (resource/name : aResource)], rules=[resource=resource/usage = read]]
			3 = Policy[party=[(name : Bob), (role : Provider), (resource/name : aResource)], rules=[resource=resource/usage = read or resource/usage = write]]
			4 = Policy[party=[(name : Carl), (role : SpecialProvider), (resource/name : aResource)], rules=[resource=resource/access = read]]
			""");
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/usage", "write"),
				allSuchThat(new Attributes()
					.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/usage : write)], from=allSuchThat: [(role : Provider)]]
			  finding matching policies
			    2: true match([(role : Provider)], [(role : Provider), (resource/name : aResource)])
			    3: true match([(role : Provider)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			    4: false match([(role : Provider)], [(name : Carl), (role : SpecialProvider), (resource/name : aResource)])
			  2: expression resource/usage = read -> false
			result: false
			"""
		);
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/usage", "read"),
				allSuchThat(new Attributes()
					.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/usage : read)], from=allSuchThat: [(role : Provider)]]
			  finding matching policies
			    2: true match([(role : Provider)], [(role : Provider), (resource/name : aResource)])
			    3: true match([(role : Provider)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			    4: false match([(role : Provider)], [(name : Carl), (role : SpecialProvider), (resource/name : aResource)])
			  2: expression resource/usage = read -> true
			  3: expression resource/usage = read or resource/usage = write -> true
			result: true
			"""
		);
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/usage", "write"),
				anySuchThat(new Attributes()
					.add("role", "Provider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/usage : write)], from=anySuchThat: [(role : Provider)]]
			  finding matching policies
			    2: true match([(role : Provider)], [(role : Provider), (resource/name : aResource)])
			    3: true match([(role : Provider)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			    4: false match([(role : Provider)], [(name : Carl), (role : SpecialProvider), (resource/name : aResource)])
			  2: expression resource/usage = read -> false
			  3: expression resource/usage = read or resource/usage = write -> true
			result: true
			"""
		);
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/usage", "write"),
				anySuchThat(new Attributes()
					.add("role", "SpecialProvider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/usage : write)], from=anySuchThat: [(role : SpecialProvider)]]
			  finding matching policies
			    2: false match([(role : SpecialProvider)], [(role : Provider), (resource/name : aResource)])
			    3: false match([(role : SpecialProvider)], [(name : Bob), (role : Provider), (resource/name : aResource)])
			    4: true match([(role : SpecialProvider)], [(name : Carl), (role : SpecialProvider), (resource/name : aResource)])
			  4: expression resource/access = read -> false: Undefined name: resource/access
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
					.add("role", "PrinterProvider")
					.add("resource/type", "printer"),
				new Rules()
					.add(new Rule(TRUE,
						new Exchange(
							requester(),
							new Attributes()
								.add("resource/type", "paper"),
							me())))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider")
					.add("resource/type", "paper"),
				new Rules()
					.add(new Rule(TRUE))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice), (role : PrinterProvider), (resource/type : printer)], rules=[resource=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]]]
			2 = Policy[party=[(name : Bob), (role : PaperProvider), (resource/type : paper)], rules=[resource=true]]
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
			    1: true match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider), (resource/type : printer)])
			  1: expression true -> true
			  1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			  evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			    2: expression true -> true
			  result: true
			result: true
			"""
		);
	}

	@Test
	void simpleMutualExchange() {
		// Alice gives printer provided the requester gives paper
		// Bob gives paper provided the requester gives printer
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider")
					.add("resource/type", "printer"),
				new Rules()
					.add(new Rule(TRUE,
						new Exchange(
							requester(),
							new Attributes()
								.add("resource/type", "paper"),
							me())))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("name", "Bob")
					.add("role", "PaperProvider")
					.add("resource/type", "paper"),
				new Rules()
					.add(new Rule(TRUE,
						new Exchange(
							requester(),
							new Attributes()
								.add("resource/type", "printer"),
							me())))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice), (role : PrinterProvider), (resource/type : printer)], rules=[resource=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]]]
			2 = Policy[party=[(name : Bob), (role : PaperProvider), (resource/type : paper)], rules=[resource=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]]]
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
			    1: true match([(role : PrinterProvider)], [(name : Alice), (role : PrinterProvider), (resource/type : printer)])
			  1: expression true -> true
			  1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			  evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			    2: expression true -> true
			    2: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			    2: satisfied Request[requester=2, resource=[(resource/type : printer)], from=1]
			  result: true
			result: true
			"""
		);
	}

	@Test
	void simpleMutualExchange2() {
		// Alice gives printer provided the requester gives paper
		// Bob gives white paper provided the requester gives printer
		// Carl gives paper provided the requester gives printer
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider")
					.add("resource/type", "printer"),
				new Rules()
					.add(new Rule(TRUE,
						new Exchange(
							requester(),
							new Attributes()
								.add("resource/type", "paper"),
							me())))))
			.add(
				new Policy( // index 2
					new Attributes()
						.add("name", "Bob")
						.add("role", "PaperProvider")
						.add("resource/type", "paper"),
					new Rules()
						.add(new Rule(
							new ExpressionWithDescription(
								c -> "white".equals(c.attribute("paper/color")),
								"paper/color = white"
							),
							new Exchange(
								requester(),
								new Attributes()
									.add("resource/type", "printer"),
								me()
							))
						)))
			.add(
				new Policy( // index 3
					new Attributes()
						.add("name", "Carl")
						.add("role", "PaperProvider")
						.add("resource/type", "paper"),
					new Rules()
						.add(new Rule(TRUE,
							new Exchange(
								requester(),
								new Attributes()
									.add("resource/type", "printer"),
								me())))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice), (role : PrinterProvider), (resource/type : printer)], rules=[resource=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]]]
			2 = Policy[party=[(name : Bob), (role : PaperProvider), (resource/type : paper)], rules=[resource=paper/color = white, exchange=Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]]]
			3 = Policy[party=[(name : Carl), (role : PaperProvider), (resource/type : paper)], rules=[resource=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]]]
			""");
		// Alice gets white paper from Carl
		// not from Bob, because he only gives white paper
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
			    2: true match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider), (resource/type : paper)])
			    3: true match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider), (resource/type : paper)])
			  2: expression paper/color = white -> false: Undefined name: paper/color
			  3: expression true -> true
			  3: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			  evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			    1: expression true -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			    1: satisfied Request[requester=1, resource=[(resource/type : paper)], from=3]
			  result: true
			result: true
			"""
		);
		// Alice gets paper from Carl
		// not from Bob, because he wants printer from requester (Alice)
		// Alice gives printer to who gives paper, but Bob only gives white paper
		// NOTE: Alice had originally requested white paper, but Carl's expression is always true
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/type", "paper")
					.add("paper/color", "white"),
				anySuchThat(new Attributes()
					.add("role", "PaperProvider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/type : paper), (paper/color : white)], from=anySuchThat: [(role : PaperProvider)]]
			  finding matching policies
			    2: true match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider), (resource/type : paper)])
			    3: true match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider), (resource/type : paper)])
			  2: expression paper/color = white -> true
			  2: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			  evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    1: expression true -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			      2: expression paper/color = white -> false: Undefined name: paper/color
			    result: false
			  result: false
			  3: expression true -> true
			  3: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			  evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			    1: expression true -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			      3: expression true -> true
			      3: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			      3: satisfied Request[requester=3, resource=[(resource/type : printer)], from=1]
			    result: true
			  result: true
			result: true
			"""
		);
		// for the above reasons, with allSuchThat it fails
		assertResultFalse(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/type", "paper")
					.add("paper/color", "white"),
				allSuchThat(new Attributes()
					.add("role", "PaperProvider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/type : paper), (paper/color : white)], from=allSuchThat: [(role : PaperProvider)]]
			  finding matching policies
			    2: true match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider), (resource/type : paper)])
			    3: true match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider), (resource/type : paper)])
			  2: expression paper/color = white -> true
			  2: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			  evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    1: expression true -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			      2: expression paper/color = white -> false: Undefined name: paper/color
			    result: false
			  result: false
			result: false
			"""
		);
	}

	@Test
	void simpleMutualExchange3() {
		// Alice gives printer provided the requester gives paper or white paper
		// Bob gives white paper provided the requester gives printer
		// Carl gives paper provided the requester gives printer
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider")
					.add("resource/type", "printer"),
				new Rules()
					.add(new Rule(TRUE,
						new Exchange(
							requester(),
							new Attributes()
								.add("resource/type", "paper"),
							me()
						)
					))
					.add(new Rule(TRUE,
						new Exchange(
							requester(),
							new Attributes()
								.add("resource/type", "paper")
								.add("paper/color", "white"),
							me()
						)
					))))
			.add(
				new Policy( // index 2
					new Attributes()
						.add("name", "Bob")
						.add("role", "PaperProvider")
						.add("resource/type", "paper"),
					new Rules()
						.add(new Rule(
							new ExpressionWithDescription(
								c -> "white".equals(c.attribute("paper/color")),
								"paper/color = white"
							),
							new Exchange(
								requester(),
								new Attributes()
									.add("resource/type", "printer"),
								me()
							))
						)))
			.add(
				new Policy( // index 3
					new Attributes()
						.add("name", "Carl")
						.add("role", "PaperProvider")
						.add("resource/type", "paper"),
					new Rules()
						.add(new Rule(TRUE,
							new Exchange(
								requester(),
								new Attributes()
									.add("resource/type", "printer"),
								me())))));
		assertPolicies("""
			1 = Policy[party=[(name : Alice), (role : PrinterProvider), (resource/type : printer)], rules=[resource=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME], resource=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : paper), (paper/color : white)], to=ME]]]
			2 = Policy[party=[(name : Bob), (role : PaperProvider), (resource/type : paper)], rules=[resource=paper/color = white, exchange=Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]]]
			3 = Policy[party=[(name : Carl), (role : PaperProvider), (resource/type : paper)], rules=[resource=true, exchange=Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]]]
			""");
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
			    2: true match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider), (resource/type : paper)])
			    3: true match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider), (resource/type : paper)])
			  2: expression paper/color = white -> false: Undefined name: paper/color
			  3: expression true -> true
			  3: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			  evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			    1: expression true -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			    1: satisfied Request[requester=1, resource=[(resource/type : paper)], from=3]
			  result: true
			result: true
			"""
		);
		// Alice gets paper from Bob first
		// Bob he wants printer from requester (Alice)
		// Alice gives printer to who gives paper or white paper, and Bob only gives white paper
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/type", "paper")
					.add("paper/color", "white"),
				anySuchThat(new Attributes()
					.add("role", "PaperProvider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/type : paper), (paper/color : white)], from=anySuchThat: [(role : PaperProvider)]]
			  finding matching policies
			    2: true match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider), (resource/type : paper)])
			    3: true match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider), (resource/type : paper)])
			  2: expression paper/color = white -> true
			  2: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			  evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    1: expression true -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			      2: expression paper/color = white -> false: Undefined name: paper/color
			    result: false
			    1: expression true -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper), (paper/color : white)], to=ME]
			    1: satisfied Request[requester=1, resource=[(resource/type : paper), (paper/color : white)], from=2]
			  result: true
			result: true
			"""
		);
		// for the above reasons, with allSuchThat it succeeds
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/type", "paper")
					.add("paper/color", "white"),
				allSuchThat(new Attributes()
					.add("role", "PaperProvider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/type : paper), (paper/color : white)], from=allSuchThat: [(role : PaperProvider)]]
			  finding matching policies
			    2: true match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider), (resource/type : paper)])
			    3: true match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider), (resource/type : paper)])
			  2: expression paper/color = white -> true
			  2: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			  evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    1: expression true -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			      2: expression paper/color = white -> false: Undefined name: paper/color
			    result: false
			    1: expression true -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper), (paper/color : white)], to=ME]
			    1: satisfied Request[requester=1, resource=[(resource/type : paper), (paper/color : white)], from=2]
			  result: true
			  3: expression true -> true
			  3: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			  evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			    1: expression true -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			      3: expression true -> true
			      3: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			      3: satisfied Request[requester=3, resource=[(resource/type : printer)], from=1]
			    result: true
			  result: true
			result: true
			"""
		);
	}

	@Test
	void simpleMutualExchange4() {
		// Alice gives printer provided the requester gives paper
		// Bob gives white paper provided the requester gives printer
		// Carl gives paper provided the requester gives printer
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("name", "Alice")
					.add("role", "PrinterProvider"),
				new Rules()
					.add(new Rule(
						new ExpressionWithDescription(
							c -> "printer".equals(c.attribute("resource/type")),
							"resource/type = printer"
						),
						new Exchange(
							requester(),
							new Attributes()
								.add("resource/type", "paper"),
							me())))))
			.add(
				new Policy( // index 2
					new Attributes()
						.add("name", "Bob")
						.add("role", "PaperProvider"),
					new Rules()
						.add(new Rule(
							new ExpressionWithDescription(
								c -> 
									"white".equals(c.attribute("paper/color")) &&
									"paper".equals(c.attribute("resource/type")),
								"paper/color = white AND resource/type = paper"
							),
							new Exchange(
								requester(),
								new Attributes()
									.add("resource/type", "printer"),
								me()
							))
						)))
			.add(
				new Policy( // index 3
					new Attributes()
						.add("name", "Carl")
						.add("role", "PaperProvider"),
					new Rules()
						.add(new Rule(
							new ExpressionWithDescription(
								c -> "paper".equals(c.attribute("resource/type")),
								"resource/type = paper"
							),
							new Exchange(
								requester(),
								new Attributes()
									.add("resource/type", "printer"),
								me())))));
		assertPolicies("""
		1 = Policy[party=[(name : Alice), (role : PrinterProvider)], rules=[resource=resource/type = printer, exchange=Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]]]
		2 = Policy[party=[(name : Bob), (role : PaperProvider)], rules=[resource=paper/color = white AND resource/type = paper, exchange=Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]]]
		3 = Policy[party=[(name : Carl), (role : PaperProvider)], rules=[resource=resource/type = paper, exchange=Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]]]
		""");

		// the following are unexpectedly true:
		// paper/color white is not provided by Carl who satisfies the exchange
		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/type", "paper")
					.add("paper/color", "white"),
				anySuchThat(new Attributes()
					.add("role", "PaperProvider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/type : paper), (paper/color : white)], from=anySuchThat: [(role : PaperProvider)]]
			  finding matching policies
			    2: true match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider)])
			    3: true match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider)])
			  2: expression paper/color = white AND resource/type = paper -> true
			  2: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			  evaluating Request[requester=2, resource=[(resource/type : printer)], from=1]
			    1: expression resource/type = printer -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=2]
			      2: expression paper/color = white AND resource/type = paper -> false: Undefined name: paper/color
			    result: false
			  result: false
			  3: expression resource/type = paper -> true
			  3: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			  evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			    1: expression resource/type = printer -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			      3: expression resource/type = paper -> true
			      3: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			      3: satisfied Request[requester=3, resource=[(resource/type : printer)], from=1]
			    result: true
			  result: true
			result: true
			"""
		);

		assertResultTrue(
			new Request(
				index(1), // Alice
				new Attributes()
					.add("resource/type", "paper")
					.add("foo", "bar"),
				anySuchThat(new Attributes()
					.add("role", "PaperProvider"))
			),
			"""
			evaluating Request[requester=1, resource=[(resource/type : paper), (foo : bar)], from=anySuchThat: [(role : PaperProvider)]]
			  finding matching policies
			    2: true match([(role : PaperProvider)], [(name : Bob), (role : PaperProvider)])
			    3: true match([(role : PaperProvider)], [(name : Carl), (role : PaperProvider)])
			  2: expression paper/color = white AND resource/type = paper -> false: Undefined name: paper/color
			  3: expression resource/type = paper -> true
			  3: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			  evaluating Request[requester=3, resource=[(resource/type : printer)], from=1]
			    1: expression resource/type = printer -> true
			    1: evaluating Exchange[from=REQUESTER, resource=[(resource/type : paper)], to=ME]
			    evaluating Request[requester=1, resource=[(resource/type : paper)], from=3]
			      3: expression resource/type = paper -> true
			      3: evaluating Exchange[from=REQUESTER, resource=[(resource/type : printer)], to=ME]
			      3: satisfied Request[requester=3, resource=[(resource/type : printer)], from=1]
			    result: true
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

	private void assertResultFalse(Request request, String expectedTrace) {
		assertAll(
			() -> assertFalse(semantics.evaluate(request)),
			() -> assertEquals(expectedTrace, semantics.getTrace().toString())
		);
	}
}
