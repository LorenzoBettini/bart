package bart.core.examples;

import static bart.core.Participants.any;
import static bart.core.Participants.index;
import static bart.core.Participants.me;
import static bart.core.Participants.requester;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import bart.core.Attributes;
import bart.core.ContextHandler;
import bart.core.ExpressionWithDescription;
import bart.core.OrExchange;
import bart.core.Policies;
import bart.core.Policy;
import bart.core.Request;
import bart.core.Result;
import bart.core.Rule;
import bart.core.Rules;
import bart.core.SingleExchange;
import bart.core.semantics.Semantics;

public class StudentsExample {

	public static void main(String[] args) {
		Policies policies = new Policies();
		Semantics semantics = new Semantics(policies);

		// add environmental information to context handler
		semantics.contextHandler(new ContextHandler()
			.add(1, "friends", List.of("ahsley", "david"))
			.add(2, "friend", List.of("david", "linda", "steven"))
		);

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

		Request request = new Request(
			index(1), // John
			new Attributes()
				.add("type", "lectureNotes")
				.add("course", "ads"),
			any(new Attributes()
				.add("studyLevel", "undergraduate")
				.add("degreeSubject", "cs")
				.add("university", "unifi"))
		);

		Result result = semantics.evaluate(request);

		System.out.println("PERMITTED: " + result.isPermitted());
		System.out.println();
		System.out.println("REQUESTS:\n" + result.getRequests().stream().map(Object::toString).collect(Collectors.joining("\n")));
		System.out.println();
		System.out.println("TRACE;\n" + semantics.getTrace().toString());
	}
}
