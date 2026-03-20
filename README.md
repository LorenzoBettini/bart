# Bart: A Specification Language for Bartering Access to Resources

[![Build Linux](https://github.com/LorenzoBettini/bart/actions/workflows/maven.yml/badge.svg)](https://github.com/LorenzoBettini/bart/actions/workflows/maven.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=LorenzoBettini_bart&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=LorenzoBettini_bart)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=LorenzoBettini_bart&metric=coverage)](https://sonarcloud.io/summary/new_code?id=LorenzoBettini_bart)

## Table of Contents

- [Overview](#overview)
- [Adding Bart to Your Maven Project](#adding-bart-to-your-maven-project)
- [Core Concepts](#core-concepts)
  - [Attributes](#attributes)
  - [Participants](#participants)
  - [Policies and Rules](#policies-and-rules)
  - [Exchanges](#exchanges)
  - [Conditions](#conditions)
  - [Context Handler](#context-handler)
  - [Semantics (Evaluation Engine)](#semantics-evaluation-engine)
  - [Result](#result)
  - [Trace](#trace)
- [Feature Examples](#feature-examples)
  - [Unconditional Resource Access](#unconditional-resource-access)
  - [Conditional Access with Expressions](#conditional-access-with-expressions)
  - [Simple Exchange (I give you this, you give me that)](#simple-exchange)
  - [OR Exchange (Either of two exchanges suffices)](#or-exchange)
  - [AND Exchange (Both exchanges must succeed)](#and-exchange)
  - [Quantified Participants: any and all](#quantified-participants-any-and-all)
  - [Context-Dependent Conditions](#context-dependent-conditions)
  - [Requester-Based Conditions](#requester-based-conditions)
  - [Real-World Example: Courier Services](#real-world-example-courier-services)
  - [Real-World Example: Student Resource Sharing](#real-world-example-student-resource-sharing)
- [Building Locally](#building-locally)
  - [Prerequisites](#prerequisites)
  - [Standard Build and Tests](#standard-build-and-tests)
  - [Code Coverage](#code-coverage)
  - [Mutation Testing](#mutation-testing)
  - [Performance Tests](#performance-tests)

---

## Overview

**Bart** is a Java 21 library that implements a semantic engine for evaluating conditional resource exchanges between participants. It models complex multi-party scenarios where participants can request resources from others, subject to policies that define what is provided, under what conditions, and what must be given in return.

Key characteristics:

- **Policy-based**: Each participant has a policy with rules that define what resources they provide and what they require in exchange.
- **Conditional**: Rules can include boolean expressions evaluated at runtime, drawing on request attributes, party attributes, and external context.
- **Compositional**: Exchanges can be combined with `AND` (both must succeed) and `OR` (either suffices) operators.
- **Traceable**: Every evaluation produces a detailed trace useful for debugging and understanding why a request was permitted or denied.
- **Extensible**: Custom `RequestComply` strategies allow fine-tuning how the engine de-duplicates exchange requests.

---

## Adding Bart to Your Maven Project

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.lorenzobettini.bart</groupId>
    <artifactId>bart.core</artifactId>
    <version>0.0.1</version>
</dependency>
```

Bart requires **Java 21** or later.

---

## Core Concepts

### Attributes

`Attributes` is a key-value container used to describe resources, parties, and conditions. Keys are unique strings; values can be any `Object`. Insertion order is preserved.

```java
var resource = new Attributes()
    .add("resource/type", "printer")
    .add("color", "laser");

Object type = resource.name("resource/type"); // "printer"
```

Duplicate keys throw `IllegalArgumentException`.

---

### Participants

Participants identify parties in requests and exchanges. Use the static factory methods on `Participants`:

| Factory method | Type | Description |
|---|---|---|
| `index(int)` | `IndexParticipant` | A specific party identified by its 1-based position in the `Policies` list |
| `me()` | `MeParticipant` | The owner of the policy currently being evaluated |
| `requester()` | `RequesterParticipant` | The party who initiated the original request |
| `any(Attributes)` | `QuantifiedParticipant` | Any party whose policy attributes match the given attributes |
| `all(Attributes)` | `QuantifiedParticipant` | All parties whose policy attributes match the given attributes |

```java
import static bart.core.Participants.*;

// Specific party by index
var alice = index(1);

// "Me" and "requester" in exchange definitions
var exchange = new SingleExchange(me(), new Attributes().add("resource/type", "paper"), requester());

// Any party with role "Provider"
var provider = any(new Attributes().add("role", "Provider"));
```

---

### Policies and Rules

A `Policy` pairs a party's identifying `Attributes` with a set of `Rules`.

A `Rule` specifies:
1. A **resource pattern** — the `Attributes` an incoming request must match (subset matching).
2. An optional **condition** (`ExpressionCode`) — a boolean expression evaluated at runtime.
3. An optional **exchange** — what the policy owner requires in return before granting access.

Rules are evaluated in order; the first matching rule wins.

```java
// A policy for "Alice" with two rules
var policy = new Policy(
    new Attributes().add("name", "Alice"),
    new Rules()
        // Rule 1: provide "printer" unconditionally
        .add(new Rule(new Attributes().add("resource/type", "printer")))
        // Rule 2: provide "scanner" only in exchange for paper
        .add(new Rule(
            new Attributes().add("resource/type", "scanner"),
            new SingleExchange(
                me(),
                new Attributes().add("resource/type", "paper"),
                requester())))
);
```

**Rule constructors** (all combinations from most to least specific):

```java
new Rule()                                          // matches anything, true condition, no exchange
new Rule(resource)                                  // matches resource, true condition, no exchange
new Rule(resource, condition)                       // matches resource, custom condition, no exchange
new Rule(resource, exchange)                        // matches resource, true condition, with exchange
new Rule(resource, condition, exchange)             // fully specified
```

A `Policies` collection holds all policies and assigns them 1-based indexes:

```java
var policies = new Policies()
    .add(alicePolicy)   // index 1
    .add(bobPolicy)     // index 2
    .add(carlPolicy);   // index 3
```

---

### Exchanges

Exchanges specify what a policy owner requires before granting access. Three types are available:

#### `SingleExchange`

A direct exchange: the party identified by `from` must provide `resource` to the party identified by `to`.

```java
// The requester must give "paper" to the policy owner ("me")
new SingleExchange(
    me(),
    new Attributes().add("resource/type", "paper"),
    requester())
```

#### `AndExchange`

Both the left and right sub-exchanges must succeed. If the left fails, the right is not attempted.

```java
new AndExchange(
    new SingleExchange(me(), new Attributes().add("document/type", "passport"), requester()),
    new SingleExchange(me(), new Attributes().add("resource/type", "payment"), requester()))
```

#### `OrExchange`

Either the left or the right sub-exchange must succeed. The left is tried first; the right is tried only if the left fails.

```java
new OrExchange(
    new SingleExchange(me(), new Attributes().add("payment/type", "cash"), requester()),
    new SingleExchange(me(), new Attributes().add("payment/type", "card"), requester()))
```

Exchanges can be nested arbitrarily to model complex multi-party scenarios.

---

### Conditions

Conditions are instances of `ExpressionCode`, a functional interface that receives a `NameResolver` and returns a boolean:

```java
ExpressionCode condition = ctx -> ctx.name("time", Integer.class) < 18;
```

Always wrap conditions in `ExpressionWithDescription` so that the trace shows a meaningful label instead of a lambda reference:

```java
var condition = new ExpressionWithDescription(
    ctx -> ctx.name("time", Integer.class) < 18,
    "time < 18");
```

The `NameResolver` provides lookup methods:

| Method | Resolves from |
|---|---|
| `name(String)` | The `from` party's request resource, context, and policy attributes |
| `name(String, Class<T>)` | Same, cast to `T` |
| `nameFromRequester(String)` | The requester party's attributes |
| `nameFromRequester(String, Class<T>)` | Same, cast to `T` |
| `nameFromParty(String, Attributes)` | The first party matching the given attributes |
| `nameFromParty(String, Attributes, Class<T>)` | Same, cast to `T` |

If a name cannot be resolved, an `UndefinedName` exception is thrown, which the engine treats as a `false` condition.

---

### Context Handler

`ContextHandler` stores dynamic, per-party contextual attributes that supplement the static policy attributes during evaluation. Values can be plain objects or lazy `Supplier` instances (called each time the attribute is read, useful for time-sensitive values).

```java
var context = new ContextHandler()
    .add(1, "location", "warehouse")              // static value for party 1
    .add(2, "time", () -> LocalTime.now().getHour()); // lazy value for party 2

semantics.contextHandler(context);
```

Attribute lookup order during condition evaluation:
1. Request resource attributes
2. Context handler attributes for the relevant party
3. Party attributes from the policy

---

### Semantics (Evaluation Engine)

`Semantics` is the core engine. Construct it with a `Policies` instance and call `evaluate(Request)`:

```java
var semantics = new Semantics(policies);
var result = semantics.evaluate(request);
```

The fluent API allows configuration chaining:

```java
semantics
    .contextHandler(new ContextHandler().add(1, "city", "London"))
    .requestComply(myCustomRequestComply);
```

A `Request` has three components:

```java
var request = new Request(
    index(1),                                           // requester (party 1)
    new Attributes().add("resource/type", "printer"),   // the requested resource
    any(new Attributes().add("role", "Provider")));     // who to request it from
```

Evaluation proceeds as follows:
1. Locate the policy (or policies) for the `from` participant.
2. For each candidate policy, iterate its rules in order.
3. The first rule whose resource pattern matches and whose condition holds triggers recursive evaluation of the required exchange.
4. Exchange evaluation resolves concrete party indexes, generates sub-requests, and checks them against already-collected requests before recursing.

---

### Result

`evaluate()` returns a `Result`:

```java
var result = semantics.evaluate(request);

boolean permitted = result.isPermitted();
Collection<Request> satisfiedRequests = result.getRequests();
```

When permitted, `getRequests()` returns the complete chain of sub-requests that were generated and satisfied during evaluation — the full exchange chain that enables access.

---

### Trace

After each evaluation, a detailed trace is available for debugging:

```java
System.out.print(semantics.getTrace());
```

The trace is reset on each call to `evaluate()`. It shows every policy and rule checked, attribute match results, condition evaluations, and exchange resolution steps.

---

## Feature Examples

### Unconditional Resource Access

The simplest case: a party provides a resource to any requester with no conditions or exchanges.

```java
var policies = new Policies()
    .add(new Policy(           // index 1 — Alice
        new Attributes().add("name", "Alice"),
        new Rules()))
    .add(new Policy(           // index 2 — Bob
        new Attributes().add("name", "Bob"),
        new Rules()
            .add(new Rule(new Attributes().add("resource/type", "printer")))));

var semantics = new Semantics(policies);
var result = semantics.evaluate(new Request(
    index(1),                                         // Alice requests
    new Attributes().add("resource/type", "printer"),
    index(2)));                                       // from Bob

System.out.println(result.isPermitted()); // true
```

---

### Conditional Access with Expressions

A rule can include a condition that must hold before access is granted. Here, Faye grants access to "white paper" only when her `current/city` attribute (provided via the context handler) equals "Firenze":

```java
var policies = new Policies()
    .add(new Policy(new Attributes().add("name", "Alice"), new Rules()))
    .add(new Policy(
        new Attributes().add("name", "Faye"),
        new Rules().add(new Rule(
            new Attributes().add("paper", "white"),
            new ExpressionWithDescription(
                ctx -> ctx.name("current/city").equals("Firenze"),
                "current/city = Firenze")))));

var semantics = new Semantics(policies);
semantics.contextHandler(new ContextHandler().add(2, "current/city", "Firenze"));

var result = semantics.evaluate(new Request(
    index(1),
    new Attributes().add("paper", "white"),
    index(2)));

System.out.println(result.isPermitted()); // true
```

---

### Simple Exchange

Alice provides a "printer" to Bob only if Bob provides "paper" in return. The engine recursively checks whether Bob can provide paper before granting the printer.

```java
var policies = new Policies()
    .add(new Policy(           // index 1 — Alice
        new Attributes().add("name", "Alice"),
        new Rules().add(new Rule(
            new Attributes().add("resource/type", "printer"),
            new SingleExchange(
                me(),
                new Attributes().add("resource/type", "paper"),
                requester())))))
    .add(new Policy(           // index 2 — Bob
        new Attributes().add("name", "Bob"),
        new Rules().add(new Rule(
            new Attributes().add("resource/type", "paper")))));

var semantics = new Semantics(policies);
var result = semantics.evaluate(new Request(
    index(2),                                         // Bob requests
    new Attributes().add("resource/type", "printer"),
    index(1)));                                       // from Alice

System.out.println(result.isPermitted()); // true
// result.getRequests() contains both:
//   Request[requester=2, resource=[(resource/type : printer)], from=1]
//   Request[requester=1, resource=[(resource/type : paper)], from=2]
```

---

### OR Exchange

FastAndFurious provides Prato address info in exchange for **either** Pistoia or Lucca address info from the requester. The engine tries Pistoia first; if that fails it tries Lucca.

```java
new Policy(
    new Attributes().add("service", "delivery").add("company", "FastAndFurious"),
    new Rules().add(new Rule(
        new Attributes().add("type", "addrInfo").add("city", "Prato"),
        new OrExchange(
            new SingleExchange(
                me(),
                new Attributes().add("type", "addrInfo").add("city", "Pistoia"),
                requester()),
            new SingleExchange(
                me(),
                new Attributes().add("type", "addrInfo").add("city", "Lucca"),
                requester())))))
```

---

### AND Exchange

FastAndFurious requires **both** Lucca and Grosseto address info from the requester (via any RabbitService party) before providing Prato address info:

```java
new Policy(
    new Attributes().add("service", "delivery").add("company", "FastAndFurious"),
    new Rules().add(new Rule(
        new Attributes().add("type", "addrInfo").add("city", "Prato"),
        new AndExchange(
            new SingleExchange(
                me(),
                new Attributes().add("type", "addrInfo").add("city", "Lucca"),
                any(new Attributes().add("service", "delivery").add("company", "RabbitService"))),
            new SingleExchange(
                me(),
                new Attributes().add("type", "addrInfo").add("city", "Grosseto"),
                any(new Attributes().add("service", "delivery").add("company", "RabbitService")))))))
```

---

### Quantified Participants: any and all

Use `any(Attributes)` to direct a request to the first matching party, or `all(Attributes)` to require all matching parties to satisfy the request.

```java
// Request from any party with role "Provider"
var request = new Request(
    index(1),
    new Attributes().add("resource/type", "printer"),
    any(new Attributes().add("role", "Provider")));

// Require ALL parties with role "Provider" to comply
var request2 = new Request(
    index(1),
    new Attributes().add("resource/type", "printer"),
    all(new Attributes().add("role", "Provider")));
```

When `any` is used, the engine searches the policy list and uses the first policy whose party attributes are a superset of the given attributes. When `all` is used, every matching policy must permit the request.

---

### Context-Dependent Conditions

Use `ContextHandler` to inject runtime state (e.g., current time or location) that conditions can then check:

```java
var context = new ContextHandler()
    .add(2, "current/city", () -> getCurrentCity()); // lazy supplier

semantics.contextHandler(context);

// The rule's condition checks ctx.name("current/city")
// The supplier is called on each evaluation
```

---

### Requester-Based Conditions

Conditions can also inspect the **requester's** attributes using `nameFromRequester(...)`. For example, Carl provides white paper only to Bob:

```java
new Policy(
    new Attributes().add("name", "Carl"),
    new Rules().add(new Rule(
        new Attributes().add("paper", "white"),
        new ExpressionWithDescription(
            ctx -> ctx.nameFromRequester("name").equals("Bob"),
            "name.requester = Bob"))))
```

---

### Real-World Example: Courier Services

The following scenario models two courier companies exchanging address information. RabbitService (party 1) covers Lucca; FastAndFurious (party 2) covers Prato but requires Lucca or Pistoia address info in return.

```java
var policies = new Policies()
    .add(new Policy(           // index 1 — RabbitService
        new Attributes()
            .add("service", "delivery")
            .add("company", "RabbitService"),
        new Rules().add(new Rule(
            new Attributes().add("type", "addrInfo").add("city", "Lucca")))))
    .add(new Policy(           // index 2 — FastAndFurious
        new Attributes()
            .add("service", "delivery")
            .add("company", "FastAndFurious"),
        new Rules().add(new Rule(
            new Attributes().add("type", "addrInfo").add("city", "Prato"),
            new OrExchange(
                new SingleExchange(
                    me(),
                    new Attributes().add("type", "addrInfo").add("city", "Pistoia"),
                    requester()),
                new SingleExchange(
                    me(),
                    new Attributes().add("type", "addrInfo").add("city", "Lucca"),
                    requester()))))));

var semantics = new Semantics(policies);
var result = semantics.evaluate(new Request(
    index(1),   // RabbitService requests Prato address info
    new Attributes().add("type", "addrInfo").add("city", "Prato"),
    any(new Attributes().add("service", "delivery").add("company", "FastAndFurious"))));

System.out.println(result.isPermitted()); // true
System.out.print(semantics.getTrace());
```

The trace would show that FastAndFurious first tries requesting Pistoia info from RabbitService (fails — RabbitService only has Lucca), then tries Lucca (succeeds), so the overall exchange is granted.

---

### Real-World Example: Student Resource Sharing

Students share lecture notes and exercises in a peer-to-peer fashion. Each student's policy defines what they share and under what conditions. Here, Mary (party 2) provides ADS lecture notes in exchange for either exercises or lecture notes from the requester. John (party 1) owns programming lecture notes without conditions.

```java
var semantics = new Semantics(policies);
semantics.contextHandler(new ContextHandler()
    .add(1, "friends", List.of("ashley", "david"))
    .add(2, "friends", List.of("david", "linda", "steven")));

policies
    .add(new Policy(           // index 1 — John
        new Attributes()
            .add("username", "john")
            .add("studyLevel", "undergraduate")
            .add("degreeProgram", "cs")
            .add("university", "unifi"),
        new Rules()
            .add(new Rule(     // shares programming lecture notes unconditionally
                new Attributes()
                    .add("type", "lectureNotes")
                    .add("course", "programming")
                    .add("teacher", "smith")
                    .add("year", "24/25")))
            .add(new Rule(     // shares programming exercises only to friends
                new Attributes()
                    .add("type", "exercises")
                    .add("course", "programming")
                    .add("year", "24/25"),
                new ExpressionWithDescription(
                    ctx -> ctx.name("friends", Collection.class)
                               .contains(ctx.nameFromRequester("username")),
                    "requester.username in friends")))))
    .add(new Policy(           // index 2 — Mary
        new Attributes()
            .add("username", "mary")
            .add("studyLevel", "undergraduate")
            .add("degreeProgram", "cs")
            .add("university", "unifi"),
        new Rules()
            .add(new Rule(     // provides ADS notes in exchange for any notes or exercises
                new Attributes()
                    .add("type", "lectureNotes")
                    .add("course", "ads"),
                new OrExchange(
                    new SingleExchange(me(), new Attributes().add("type", "exercises"), requester()),
                    new SingleExchange(me(), new Attributes().add("type", "lectureNotes"), requester()))))));

// John requests ADS lecture notes from any CS student at unifi
var result = semantics.evaluate(new Request(
    index(1),
    new Attributes().add("type", "lectureNotes").add("course", "ads"),
    any(new Attributes()
        .add("studyLevel", "undergraduate")
        .add("degreeProgram", "cs")
        .add("university", "unifi"))));

System.out.println(result.isPermitted()); // true
```

The engine finds Mary, checks whether she can get exercises from John (fails — John only shares exercises with friends, and Mary is not in John's friends list), then falls back to lecture notes (succeeds — John shares programming lecture notes unconditionally).

---

## Building Locally

### Prerequisites

- [Apache Maven](https://maven.apache.org/) 3.9+
- Java 21+

### Standard Build and Tests

Clone the repository and run:

```bash
mvn clean verify
```

This compiles the code, runs all unit tests, and packages the artifact.

---

### Code Coverage

To measure code coverage with JaCoCo, activate the `jacoco` profile:

```bash
mvn -P jacoco clean verify
```

The HTML coverage report is generated at:

```
bart.core/target/site/jacoco/index.html
```

Open this file in a browser to browse coverage by class and method.

---

### Mutation Testing

Mutation testing uses [PIT](https://pitest.org/) to verify the quality of the test suite. Run it from the `bart.core` directory (or from the repository root):

```bash
cd bart.core
mvn org.pitest:pitest-maven:mutationCoverage -Dpit-timeout-const=1000
```

The HTML mutation report is generated at:

```
bart.core/target/pit-reports/index.html
```

The project enforces a **100% mutation score** for all production code. If surviving mutants are reported, additional tests must be added to kill them.

> **Tip**: Increase `-Dpit-timeout-const` if your machine is slow and PIT reports timed-out mutations.

---

### Performance Tests

Performance tests measure how evaluation time scales with the number of policies, attributes, exchange width, and exchange chain depth. Run them from the `bart.core` directory using the dedicated Maven profile:

```bash
cd bart.core
mvn test -Pperformance-tests
```

Results are printed to the console in a table showing average, minimum, maximum, and standard deviation of execution time across 100 repetitions (after a 20-iteration JVM warm-up). See [`bart.core/src/test/java/bart/core/performance/README.md`](bart.core/src/test/java/bart/core/performance/README.md) for full details on the test configuration and output format.
