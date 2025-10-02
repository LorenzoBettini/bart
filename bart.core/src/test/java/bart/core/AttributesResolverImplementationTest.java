package bart.core;

import static bart.core.Participants.index;
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
		// Note: context index is determined by request.from().getIndex() (which is 2)
		contextHandler = new ContextHandler()
			.add(0, "context/other", "value0")
			.add(1, "context/department", "IT")
			.add(2, "context/time", "morning")
			.add(2, "context/priority", "high")
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

		// Create resolver - context index determined by request.from().getIndex() (which is 2)
		resolver = new AttributesResolverImplementation(request, contextHandler, policies);
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
		// Should find attribute in from party attributes (from index 2 -> policy index 2 -> Bob's policy)
		Object result = resolver.name("name");
		assertEquals("Bob", result);
		
		result = resolver.name("role");
		assertEquals("User", result);
	}

	@Test
	void testResolutionPriority() throws UndefinedName {
		// Setup a scenario where the same attribute name exists in multiple sources
		// to test resolution priority: resource > context > from party
		
		// Add attribute to context that conflicts with resource
		contextHandler.add(2, "resource/type", "scanner");
		
		// Should return value from resource (highest priority)
		Object result = resolver.name("resource/type");
		assertEquals("printer", result);
	}

	@Test
	void testContextVsRequesterPartyPriority() throws UndefinedName {
		// Test priority between context handler and from party
		// when resource doesn't have the attribute
		
		// Add same attribute to both context and from party
		contextHandler.add(2, "common/attribute", "from_context");
		
		// Should return value from context handler (higher priority than from party)
		Object result = resolver.name("common/attribute");
		assertEquals("from_context", result);
	}

	@Test
	void testDifferentContextIndex() throws UndefinedName {
		// Test resolver with different context index by changing request.from()
		var requestFromIndex1 = new Request(
			index(3), // requester 
			new Attributes().add("resource/test", "value"),
			index(1) // from index 1 - this will be used for both context and party lookup
		);
		
		var resolverForIndex1 = new AttributesResolverImplementation(requestFromIndex1, contextHandler, policies);
		
		// Should access context for index 1 (as specified in request.from())
		contextHandler.add(1, "policy1/context", "value1");
		Object result = resolverForIndex1.name("policy1/context");
		assertEquals("value1", result);
		
		// Should access from party (index 1 -> policy index 1 -> Alice's policy)
		result = resolverForIndex1.name("name");
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
	void testNullAttributeValues() {
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
			requestWithEmpty, emptyContextHandler, policies);
		
		// Should throw UndefinedName when all sources return null
		assertThatThrownBy(() -> emptyResolver.name("nonexistent"))
			.isInstanceOf(UndefinedName.class);
	}

	@Test
	void testResolverWithEmptyAttributes() {
		// Test resolver behavior with empty attributes in various sources
		var emptyRequest = new Request(
			index(2),
			new Attributes(), // empty resource
			index(1) // from index 1 (valid policy index)
		);
		
		var emptyContextHandler = new ContextHandler(); // no attributes added
		
		var emptyPolicies = new Policies()
			.add(new Policy(new Attributes(), new Rules())); // empty party attributes
		
		var emptyResolver = new AttributesResolverImplementation(
			emptyRequest, emptyContextHandler, emptyPolicies);
		
		// Should throw UndefinedName for any attribute
		assertThatThrownBy(() -> emptyResolver.name("any/attribute"))
			.isInstanceOf(UndefinedName.class)
			.hasMessage("Undefined name: any/attribute");
	}

	@Test
	void testRequesterIndexMapping() throws UndefinedName {
		// Test that from index is correctly mapped to policy index
		// From with index 1 should map to policy index 1 (Alice's policy)
		var requestFromIndex1 = new Request(
			index(3), // requester index 3
			new Attributes().add("resource/test", "value"),
			index(1) // from index 1
		);
		
		var resolverForIndex1 = new AttributesResolverImplementation(
			requestFromIndex1, contextHandler, policies);
		
		// Should access policy at index 1 (Alice's policy) for from index 1
		Object result = resolverForIndex1.name("name");
		assertEquals("Alice", result);
	}

	@Test
	void testComplexAttributeResolution() throws UndefinedName {
		// Test complex scenario with multiple attribute types
		
		// Setup complex context
		contextHandler
			.add(2, "time/current", "10:30")
			.add(2, "location/floor", "3rd")
			.add(1, "user/preferences", "dark_mode");
		
		// Test accessing different types of attributes
		assertEquals("printer", resolver.name("resource/type"));
		assertEquals("10:30", resolver.name("time/current"));
		assertEquals("3rd", resolver.name("location/floor"));
		assertEquals("Bob", resolver.name("name")); // from party (index 2 -> Bob)
		assertEquals("User", resolver.name("role")); // from party (index 2 -> Bob)
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
			specialRequest, contextHandler, policies);
		
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

	@Test
	void testNameFromRequesterUsesRequesterIndex() throws UndefinedName {
		// Test that nameFromRequester uses requester.index() instead of from.getIndex() for context lookup
		// Setup different values in context for requester index (1) vs from index (2)
		contextHandler.add(1, "test/attribute", "from_requester");
		contextHandler.add(2, "test/attribute", "from_target");
		
		// name() should use from index (2)
		Object resultFromName = resolver.name("test/attribute");
		assertEquals("from_target", resultFromName);
		
		// nameFromRequester() should use requester index (1)
		Object resultFromRequester = resolver.nameFromRequester("test/attribute");
		assertEquals("from_requester", resultFromRequester);
	}

	@Test
	void testNameFromPartyWithMatchingParty() throws UndefinedName {
		// Test nameFromParty with attributes that match an existing party
		// Should find Alice's policy (index 1) when searching for role "Admin"
		var searchAttributes = new Attributes().add("role", "Admin");
		
		// Add context attribute for Alice's index (1) - policies are indexed starting from 1
		contextHandler.add(1, "context/level", "administrator");
		
		// Should find attribute from Alice's policy context
		Object result = resolver.nameFromParty("context/level", searchAttributes);
		assertEquals("administrator", result);
		
		// Should find attribute from Alice's party attributes
		result = resolver.nameFromParty("name", searchAttributes);
		assertEquals("Alice", result);
		
		// Should find attribute from resource (highest priority) when it exists
		result = resolver.nameFromParty("resource/type", searchAttributes);
		assertEquals("printer", result);
	}

	@Test
	void testNameFromPartyWithNoMatchingParty() throws UndefinedName {
		// Test nameFromParty when no party matches the search attributes
		var searchAttributes = new Attributes().add("role", "NonExistentRole");
		
		// Should throw UndefinedName when no party matches and attribute is not in resource
		assertThatThrownBy(() -> resolver.nameFromParty("name", searchAttributes))
			.isInstanceOf(UndefinedName.class)
			.hasMessage("Undefined name: name");
		
		// Should still find attribute from resource even when no party matches
		Object result = resolver.nameFromParty("resource/type", searchAttributes);
		assertEquals("printer", result);
	}

	@Test
	void testGenericNameWithValidCast() throws UndefinedName {
		// Test generic name method with valid type casting
		String resourceType = resolver.name("resource/type", String.class);
		assertEquals("printer", resourceType);
		
		String contextTime = resolver.name("context/time", String.class);
		assertEquals("morning", contextTime);
		
		String partyName = resolver.name("name", String.class);
		assertEquals("Bob", partyName);
	}

	@Test
	void testGenericNameWithInvalidCast() throws UndefinedName {
		// Test generic name method with invalid type casting
		// Should throw ClassCastException when trying to cast string to integer
		assertThatThrownBy(() -> resolver.name("resource/type", Integer.class))
			.isInstanceOf(ClassCastException.class);
		
		// Should throw UndefinedName for non-existent attribute
		assertThatThrownBy(() -> resolver.name("nonexistent", String.class))
			.isInstanceOf(UndefinedName.class)
			.hasMessage("Undefined name: nonexistent");
	}
}
