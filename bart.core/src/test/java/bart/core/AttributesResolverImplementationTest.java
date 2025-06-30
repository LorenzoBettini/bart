package bart.core;

import static bart.core.Participants.index;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bart.core.semantics.UndefinedName;

class AttributesResolverImplementationTest {

	private AttributesResolverImplementation resolver;
	private Request request;
	private ContextHandler contextHandler;
	private Policies policies;

	@BeforeEach
	void init() {
		// Setup request with resource attributes
		var resourceAttributes = new Attributes()
			.add("resource/type", "printer")
			.add("resource/location", "office");
		
		request = new Request(
			index(1), // requester
			resourceAttributes,
			index(2) // from
		);

		// Setup context handler with attributes for different parties
		contextHandler = new ContextHandler()
			.add(0, "context/time", "morning")
			.add(0, "context/priority", "high")
			.add(1, "context/department", "IT")
			.add(2, "context/building", "North");

		// Setup policies with party attributes
		var policy1 = new Policy(
			new Attributes()
				.add("name", "Alice")
				.add("role", "Admin"),
			new Rules()
		);
		
		var policy2 = new Policy(
			new Attributes()
				.add("name", "Bob")
				.add("role", "User")
				.add("department", "Finance"),
			new Rules()
		);

		policies = new Policies()
			.add(policy1)
			.add(policy2);

		// Create resolver for policy index 0 (Alice's policy)
		resolver = new AttributesResolverImplementation(request, contextHandler, policies, 0);
	}

	@Test
	void testResolveFromResourceAttributes() throws UndefinedName {
		// Should find attribute in request resource (first priority)
		Object result = resolver.name("resource/type");
		assertEquals("printer", result);
		
		result = resolver.name("resource/location");
		assertEquals("office", result);
	}

	@Test
	void testResolveFromContextHandler() throws UndefinedName {
		// Should find attribute in context handler for policy index 0
		Object result = resolver.name("context/time");
		assertEquals("morning", result);
		
		result = resolver.name("context/priority");
		assertEquals("high", result);
	}

	@Test
	void testResolveFromRequesterPartyAttributes() throws UndefinedName {
		// Should find attribute in requester's party attributes (requester index 1 -> policy index 0)
		Object result = resolver.name("name");
		assertEquals("Alice", result);
		
		result = resolver.name("role");
		assertEquals("Admin", result);
	}

	@Test
	void testResolutionPriority() throws UndefinedName {
		// Setup a scenario where the same attribute name exists in multiple sources
		// to test resolution priority: resource > context > requester party
		
		// Add attribute to context that conflicts with resource
		contextHandler.add(0, "resource/type", "scanner");
		
		// Add attribute to requester's party that conflicts with both
		var conflictingPolicy = new Policy(
			new Attributes()
				.add("resource/type", "copier")
				.add("name", "Alice"),
			new Rules()
		);
		
		var conflictPolicies = new Policies().add(conflictingPolicy);
		var conflictResolver = new AttributesResolverImplementation(request, contextHandler, conflictPolicies, 0);
		
		// Should return value from resource (highest priority)
		Object result = conflictResolver.name("resource/type");
		assertEquals("printer", result);
	}

	@Test
	void testContextVsRequesterPartyPriority() throws UndefinedName {
		// Test priority between context handler and requester party
		// when resource doesn't have the attribute
		
		// Add same attribute to both context and requester party
		contextHandler.add(0, "common/attribute", "from_context");
		
		var testPolicy = new Policy(
			new Attributes()
				.add("common/attribute", "from_party")
				.add("name", "TestUser"),
			new Rules()
		);
		
		var testPolicies = new Policies().add(testPolicy);
		var testResolver = new AttributesResolverImplementation(request, contextHandler, testPolicies, 0);
		
		// Should return value from context handler (higher priority than requester party)
		Object result = testResolver.name("common/attribute");
		assertEquals("from_context", result);
	}

	@Test
	void testDifferentPolicyIndex() throws UndefinedName {
		// Test resolver with different policy index
		var resolverForPolicy1 = new AttributesResolverImplementation(request, contextHandler, policies, 1);
		
		// Should access context for policy index 1 (adding to party index 1 for policy index 1)
		contextHandler.add(1, "policy1/context", "value1");
		Object result = resolverForPolicy1.name("policy1/context");
		assertEquals("value1", result);
		
		// Should still access requester's party (index 1 -> policy index 0)
		result = resolverForPolicy1.name("name");
		assertEquals("Alice", result);
	}

	@Test
	void testUndefinedNameException() {
		// Should throw UndefinedName when attribute is not found in any source
		assertThatThrownBy(() -> resolver.name("nonexistent/attribute"))
			.isInstanceOf(UndefinedName.class)
			.hasMessage("Undefined name: nonexistent/attribute");
	}

	@Test
	void testUndefinedNameExceptionMessage() {
		// Test exception message for different undefined names
		assertThatThrownBy(() -> resolver.name("unknown"))
			.isInstanceOf(UndefinedName.class)
			.hasMessage("Undefined name: unknown");
		
		assertThatThrownBy(() -> resolver.name(""))
			.isInstanceOf(UndefinedName.class)
			.hasMessage("Undefined name: ");
	}

	@Test
	void testNullAttributeValues() throws UndefinedName {
		// Test handling of null attribute values (should be filtered out)
		// Note: Attributes.add() doesn't allow null values, but we can test the filter logic
		// by having null returned from name() method indirectly
		
		var requestWithEmpty = new Request(
			index(1),
			new Attributes(), // empty resource
			index(2)
		);
		
		var emptyContextHandler = new ContextHandler();
		
		var emptyResolver = new AttributesResolverImplementation(
			requestWithEmpty, emptyContextHandler, policies, 0);
		
		// Should throw UndefinedName when all sources return null
		assertThatThrownBy(() -> emptyResolver.name("nonexistent"))
			.isInstanceOf(UndefinedName.class);
	}

	@Test
	void testResolverWithEmptyAttributes() throws UndefinedName {
		// Test resolver behavior with empty attributes in various sources
		var emptyRequest = new Request(
			index(1),
			new Attributes(), // empty resource
			index(2)
		);
		
		var emptyContextHandler = new ContextHandler(); // no attributes added
		
		var emptyPolicies = new Policies()
			.add(new Policy(new Attributes(), new Rules())); // empty party attributes
		
		var emptyResolver = new AttributesResolverImplementation(
			emptyRequest, emptyContextHandler, emptyPolicies, 0);
		
		// Should throw UndefinedName for any attribute
		assertThatThrownBy(() -> emptyResolver.name("any/attribute"))
			.isInstanceOf(UndefinedName.class)
			.hasMessage("Undefined name: any/attribute");
	}

	@Test
	void testRequesterIndexMapping() throws UndefinedName {
		// Test that requester index is correctly mapped to policy index
		// Requester with index 2 should map to policy index 1 (2-1=1)
		var requestFromIndex2 = new Request(
			index(2), // requester index 2
			new Attributes().add("resource/test", "value"),
			index(3) // from
		);
		
		var resolverForIndex2 = new AttributesResolverImplementation(
			requestFromIndex2, contextHandler, policies, 0);
		
		// Should access policy at index 1 (Bob's policy) for requester index 2
		Object result = resolverForIndex2.name("department");
		assertEquals("Finance", result);
	}

	@Test
	void testComplexAttributeResolution() throws UndefinedName {
		// Test complex scenario with multiple attribute types
		
		// Setup complex context
		contextHandler
			.add(0, "time/current", "10:30")
			.add(0, "location/floor", "3rd")
			.add(1, "user/preferences", "dark_mode");
		
		// Test accessing different types of attributes
		assertEquals("printer", resolver.name("resource/type"));
		assertEquals("10:30", resolver.name("time/current"));
		assertEquals("3rd", resolver.name("location/floor"));
		assertEquals("Alice", resolver.name("name"));
		assertEquals("Admin", resolver.name("role"));
	}

	@Test
	void testAttributeNameEdgeCases() throws UndefinedName {
		// Test various edge cases for attribute names
		var specialRequest = new Request(
			index(1),
			new Attributes()
				.add("", "empty_key")
				.add("key with spaces", "spaces")
				.add("key/with/slashes", "slashes")
				.add("123numeric", "numeric"),
			index(2)
		);
		
		var specialResolver = new AttributesResolverImplementation(
			specialRequest, contextHandler, policies, 0);
		
		// Should handle various attribute name formats
		assertEquals("empty_key", specialResolver.name(""));
		assertEquals("spaces", specialResolver.name("key with spaces"));
		assertEquals("slashes", specialResolver.name("key/with/slashes"));
		assertEquals("numeric", specialResolver.name("123numeric"));
		
		// Null parameter should throw UndefinedName, not NPE
		assertThatThrownBy(() -> specialResolver.name(null))
			.isInstanceOf(UndefinedName.class)
			.hasMessage("Undefined name: null");
	}
}
