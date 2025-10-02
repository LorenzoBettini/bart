package bart.core.examples;

import static bart.core.Participants.any;
import static bart.core.Participants.index;
import static bart.core.Participants.me;
import static bart.core.Participants.requester;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bart.core.Attributes;
import bart.core.ContextHandler;
import bart.core.ExpressionWithDescription;
import bart.core.OrExchange;
import bart.core.Policies;
import bart.core.Policy;
import bart.core.Request;
import bart.core.Rule;
import bart.core.Rules;
import bart.core.SingleExchange;
import bart.core.semantics.Semantics;

class StudentsExampleTest {

	private Semantics semantics;
	private Policies policies;

	@BeforeEach
	void init() {
		policies = new Policies();
		semantics = new Semantics(policies);
		// add environmental information to context handler
		semantics.contextHandler(new ContextHandler()
			.add(1, "friends", List.of("ahsley", "david"))
			.add(2, "friend", List.of("david", "linda", "steven"))
		);
	}

	@Test
	void firstScenario() {
		policies.add(
			new Policy( // index 1
				new Attributes()
					.add("username", "john")
					.add("studyLevel", "undergraduate")
					.add("degreeSubject", "cs")
					.add("university", "unifi")
					.add("enrollment", "2024"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "lectureNotes")
							.add("course", "programming")
							.add("teacher", "gosling")
							.add("year", "24/25")
					))
					.add(new Rule(
						new Attributes()
							.add("type", "exercises")
							.add("course", "programming")
							.add("year", "24/25"),
						new ExpressionWithDescription(
							c -> c.name("friends", Collection.class).contains(c.nameFromRequester("username")),
							"requester.username in friends"
						)
					))))
		.add(
			new Policy( // index 2
				new Attributes()
					.add("username", "mary")
					.add("studyLevel", "undergraduate")
					.add("degreeSubject", "cs")
					.add("university", "unifi")
					.add("enrollment", "2023"),
				new Rules()
					.add(new Rule(
						new Attributes()
							.add("type", "lectureNotes")
							.add("course", "ads")
							.add("teacher", "hoare")
							.add("year", "23/24"),
						new OrExchange(
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "exercises"),
								requester()),
							new SingleExchange(
								me(),
								new Attributes()
									.add("type", "lectureNotes"),
								requester())
						)
					))));

		assertResultTrue(
			new Request(
				index(1), // John
				new Attributes()
					.add("type", "lectureNotes")
					.add("course", "ads"),
				any(new Attributes()
					.add("studyLevel", "undergraduate")
					.add("degreeSubject", "cs")
					.add("university", "unifi"))
			),
			"""
			evaluating Request[requester=1, resource=[(type : lectureNotes), (course : ads)], from=any: [(studyLevel : undergraduate), (degreeSubject : cs), (university : unifi)]]
			  finding matching policies
			    policy 2: from match([(studyLevel : undergraduate), (degreeSubject : cs), (university : unifi)], [(username : mary), (studyLevel : undergraduate), (degreeSubject : cs), (university : unifi), (enrollment : 2023)]) -> true
			  policy 2: evaluating Request[requester=1, resource=[(type : lectureNotes), (course : ads)], from=2]
			    rule 2.1: resource match([(type : lectureNotes), (course : ads)], [(type : lectureNotes), (course : ads), (teacher : hoare), (year : 23/24)]) -> true
			    rule 2.1: condition true -> true
			    rule 2.1: evaluating OR(Exchange[to=ME, resource=[(type : exercises)], from=REQUESTER], Exchange[to=ME, resource=[(type : lectureNotes)], from=REQUESTER])
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : exercises)], from=REQUESTER]
			      evaluating Request[requester=2, resource=[(type : exercises)], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : exercises)], from=1]
			          rule 1.1: resource match([(type : exercises)], [(type : lectureNotes), (course : programming), (teacher : gosling), (year : 24/25)]) -> false
			        policy 1: evaluating Request[requester=2, resource=[(type : exercises)], from=1]
			          rule 1.2: resource match([(type : exercises)], [(type : exercises), (course : programming), (year : 24/25)]) -> true
			          rule 1.2: condition requester.username in friends -> false
			      result: false
			    rule 2.1: OR
			      rule 2.1: evaluating Exchange[to=ME, resource=[(type : lectureNotes)], from=REQUESTER]
			      evaluating Request[requester=2, resource=[(type : lectureNotes)], from=1]
			        policy 1: evaluating Request[requester=2, resource=[(type : lectureNotes)], from=1]
			          rule 1.1: resource match([(type : lectureNotes)], [(type : lectureNotes), (course : programming), (teacher : gosling), (year : 24/25)]) -> true
			          rule 1.1: condition true -> true
			      result: true
			    rule 2.1: END Exchange -> true
			result: true
			""",
			"""
			Request[requester=1, resource=[(type : lectureNotes), (course : ads)], from=2]
			Request[requester=2, resource=[(type : lectureNotes)], from=1]"""
		);
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


}
