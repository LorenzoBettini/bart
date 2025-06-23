package colpo.core.semantics;

import static colpo.core.Participants.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import colpo.core.AndExchange;
import colpo.core.AttributeMatcher;
import colpo.core.Attributes;
import colpo.core.CompositeExchange;
import colpo.core.ContextHandler;
import colpo.core.DefaultRequestComply;
import colpo.core.Exchange;
import colpo.core.IndexParticipant;
import colpo.core.OrExchange;
import colpo.core.Participant;
import colpo.core.Policies;
import colpo.core.Policies.PolicyData;
import colpo.core.Policy;
import colpo.core.Request;
import colpo.core.RequestComply;
import colpo.core.RequestFromParticipant;
import colpo.core.Result;
import colpo.core.Rule;
import colpo.core.Rules;
import colpo.core.SingleExchange;

/**
 * @author Lorenzo Bettini
 */
public class Semantics {

	private Policies policies;
	private AttributeMatcher matcher = new AttributeMatcher();
	private Trace trace = new Trace();
	private ContextHandler contextHandler = EMPTY_CONTEXT_HANDLER;
	private RequestComply requestComply = new DefaultRequestComply(matcher);

	private static final ContextHandler EMPTY_CONTEXT_HANDLER = new ContextHandler();
	private static final Result DENIED = new Result(false);

	public Semantics(Policies policies) {
		this.policies = policies;
	}

	public Semantics contextHandler(ContextHandler contextHandler) {
		this.contextHandler = contextHandler;
		return this;
	}

	public Semantics requestComply(RequestComply requestComply) {
		this.requestComply = requestComply;
		return this;
	}

	public Result evaluate(Request request) {
		trace.reset();
		return evaluate(request, new LinkedHashSet<>());
	}

	private Result evaluate(Request request, Set<Request> requests) {
		trace.addAndThenIndent(String.format("evaluating %s", request));
		var from = request.from();
		var index = from.getIndex();
		var result = DENIED;
		if (index > 0) {
			result = evaluate(index, policies.getByIndex(index), request, requests);
		} else {
			trace.addAndThenIndent("finding matching policies");
			var policiesToEvaluate = policiesToEvaluate(request.requester(), from);
			trace.removeIndent();
			if (!policiesToEvaluate.isEmpty()) {
				var successfullRequests = new ArrayList<Request>();
				Predicate<PolicyData> evaluatePredicate =
					d -> collectingRequests(
						evaluate(d.index(), d.policy(), request.withFrom(d.index()), requests),
						successfullRequests);
				var permitted = false;
				if (from.isAll()) {
					permitted = policiesToEvaluate.stream()
						.allMatch(evaluatePredicate);
				} else {
					permitted = policiesToEvaluate.stream()
						.anyMatch(evaluatePredicate);
				}
				if (permitted) {
					result = Result.permitted().addAll(successfullRequests);
				}
			}
		}
		trace.removeIndentAndThenAdd(String.format("result: %s", result.isPermitted()));
		return result;
	}

	private boolean collectingRequests(Result result, Collection<Request> requests) {
		var permitted = result.isPermitted();
		if (permitted) {
			requests.addAll(result.getRequests());
		}
		return permitted;
	}

	private Collection<PolicyData> policiesToEvaluate(Participant requester,
			Participant from) {
		return policies.getPolicyData()
			.filter(d -> d.index() != requester.getIndex())
			.filter(d -> {
				var attributes1 = from.getAttributes();
				var attributes2 = d.policy().party();
				return tryMatch("policy " + d.index(), "from", attributes1, attributes2);
			})
			.toList();
	}

	private boolean tryMatch(String prefix, String description, Attributes attributes1, Attributes attributes2) {
		boolean matchResult = matcher.match(attributes1, attributes2);
		trace.add(String.format("%s: %s match(%s, %s) -> %s",
			prefix, description, attributes1, attributes2, matchResult));
		return matchResult;
	}

	private Result evaluate(int policyIndex, Policy policy, Request request, Set<Request> requests) {
		return evaluate(policyIndex, policy.rules(), request, requests);
	}

	private Result evaluate(int policyIndex, Rules rules, Request request, Set<Request> requests) {
		return rules.getRuleData()
			.map(r -> evaluate(policyIndex, r.index(), r.rule(), request, requests))
			.filter(Result::isPermitted)
			.findFirst()
			.orElse(DENIED);
	}

	private Result evaluate(int policyIndex, int ruleIndex, Rule rule, Request request, Set<Request> requests) {
		trace.addAndThenIndent(String.format("policy %d: evaluating %s",
				policyIndex, request));
		try {
			boolean outcome = tryMatch(traceForRule(policyIndex, ruleIndex), "resource", request.resource(), rule.getResource());
			if (!outcome) {
				return DENIED;
			}
			outcome = rule.getCondition().evaluate(
				name -> Stream.of(
							request.resource(),
							contextHandler.ofParty(policyIndex),
							policies.getByIndex(request.requester().index()).party())
					.map(attributes -> attributes.name(name))
					.filter(Objects::nonNull)
					.findFirst()
					.orElseThrow(() -> new UndefinedName(name))
			);
			trace.add(String.format("%s: condition %s -> %s", traceForRule(policyIndex, ruleIndex), rule.getCondition(), outcome));
			if (!outcome) {
				return DENIED;
			}
			var result = evaluateExchange(policyIndex, ruleIndex, rule.getExchange(), request, requests);
			if (result.isPermitted()) {
				return Result.permitted()
						.add(request)
						.addAll(result.getRequests());
			}
			return result;
		} catch (Exception e) {
			trace.add(String.format("%s: condition %s -> %s", traceForRule(policyIndex, ruleIndex), rule.getCondition(), e.getMessage()));
			return DENIED;
		} finally {
			trace.removeIndent();
		}
	}

	private Result evaluateExchange(int policyIndex, int ruleIndex, Exchange exchange, Request request, Set<Request> requests) {
		Result result;
		requests.add(request);

		var isComposite = exchange instanceof CompositeExchange;

		if (isComposite) {
			trace.addAndThenIndent(String.format("%s: evaluating %s", traceForRule(policyIndex, ruleIndex), exchange));
		}

		switch (exchange) {
		case OrExchange(var left, var right) -> {
			result = evaluateExchange(policyIndex, ruleIndex, left, request, requests);
			if (!result.isPermitted()) {
				trace.addInPreviousIndent(String.format("%s: OR", traceForRule(policyIndex, ruleIndex)));
				result = evaluateExchange(policyIndex, ruleIndex, right, request, requests);
			}
		}
		case AndExchange(var left, var right) -> {
			result = evaluateExchange(policyIndex, ruleIndex, left, request, requests);
			if (result.isPermitted()) {
				trace.addInPreviousIndent(String.format("%s: AND", traceForRule(policyIndex, ruleIndex)));
				var result1 = evaluateExchange(policyIndex, ruleIndex, right, request, requests);
				if (result1.isPermitted()) {
					result.addAll(result1.getRequests());
				} else {
					result = DENIED;
				}
			}
		}
		case SingleExchange singleExchange -> result = evaluate(policyIndex, ruleIndex, singleExchange, request, requests);
		case null -> result = Result.permitted();
		}

		if (isComposite) {
			trace.removeIndentAndThenAdd(String.format("%s: END Exchange -> %s",
					traceForRule(policyIndex, ruleIndex), result.isPermitted()));
		}

		requests.remove(request);

		return result;
	}

	private Result evaluate(int policyIndex, int ruleIndex, SingleExchange exchange, Request request, Set<Request> requests) {
		trace.add(String.format("%s: evaluating %s", traceForRule(policyIndex, ruleIndex), exchange));

		var exchangeFrom = exchange.from();
		var exchangeTo = exchange.to();

		Collection<Integer> fromIndexes;
		Collection<Integer> toIndexes;

		if (exchangeFrom.isRequester()) {
			fromIndexes = Collections.singleton(request.requester().getIndex());
		} else {
			fromIndexes = computeIndexes(exchangeFrom.getAttributes());
		}

		if (exchangeTo.isMe()) {
			toIndexes = Collections.singleton(policyIndex);
		} else {
			toIndexes = computeIndexes(exchangeTo.getAttributes());
		}

		if (toIndexes.isEmpty()) {
			trace.add(String.format("%s: satisfied: no one to exchange", traceForRule(policyIndex, ruleIndex)));
			return Result.permitted(); // there's no one to satisfy
		}
		// this check would be implied by the later
		// atLeastOneRequest.hasBeenGenerated for from: allSuchThat
		// but this way we can give a more informative message
		if (fromIndexes.isEmpty()) {
			trace.add(String.format("%s: not satisfied: no one from exchange", traceForRule(policyIndex, ruleIndex)));
			return DENIED; // no one can satisfy
		}

		BiPredicate<Integer, Integer> differentIndexes = (i1, i2) -> !i1.equals(i2);

		Predicate<Integer> innerOperation;

		// to keep track of the fact that at least one inner
		// "loop" is executed; that's required for the semantics of allMatch
		// see below
		var atLeastOneRequest = new Object() {
			boolean hasBeenGenerated = false;
		};

		var successfullRequests = new ArrayList<Request>();
		BiPredicate<Integer, Integer> innerPredicate = (fromIndex, toIndex) -> {
			// record that at least one inner loop has been executed
			// useful for the external allMatch case
			// see below
			atLeastOneRequest.hasBeenGenerated = true;
			return collectingRequests(
				evaluateExchangeRequest(policyIndex, ruleIndex, exchange, index(toIndex), index(fromIndex), requests),
				successfullRequests);
		};

		if (exchangeTo.isAll()) {
			innerOperation = fromIndex ->
				toIndexes.stream()
					.filter(toIndex -> differentIndexes.test(toIndex, fromIndex))
					.allMatch(toIndex -> innerPredicate.test(fromIndex, toIndex));
		} else {
			innerOperation = fromIndex ->
				toIndexes.stream()
					.filter(toIndex -> differentIndexes.test(toIndex, fromIndex))
					.anyMatch(toIndex -> innerPredicate.test(fromIndex, toIndex));
		}

		var permitted = false;

		if (exchangeFrom.isAll()) {
			permitted = fromIndexes.stream()
				.allMatch(innerOperation);
			// this additional check is required because allMatch returns
			// true if the stream was empty
			if (!atLeastOneRequest.hasBeenGenerated) {
				trace.add(String.format("%s: not satisfied: no request could be generated", traceForRule(policyIndex, ruleIndex)));
				permitted = false;
			}
		} else {
			permitted = fromIndexes.stream()
				.anyMatch(innerOperation);
		}

		return permitted ? Result.permitted().addAll(successfullRequests) : DENIED;
	}

	private List<Integer> computeIndexes(Attributes attributesToMatch) {
		return policies.getPolicyData()
				.filter(d -> {
					var attributes1 = attributesToMatch;
					var attributes2 = d.policy().party();
					return tryMatch("policy " + d.index(), "from", attributes1, attributes2);
				})
				.map(PolicyData::index)
				.toList();
	}

	private Result evaluateExchangeRequest(int policyIndex,
			int ruleIndex,
			SingleExchange exchange,
			IndexParticipant exchangeRequestRequester,
			RequestFromParticipant exchangeRequestFrom,
			Set<Request> requests) {
		var exchangeRequest = new Request(
			exchangeRequestRequester,
			exchange.resource(),
			exchangeRequestFrom);
		if (requests.stream()
				.anyMatch(existingRequest -> requestComply.test(exchangeRequest, existingRequest))) {
			trace.add(String.format("%s: compliant request found %s", traceForRule(policyIndex, ruleIndex), exchangeRequest));
			return Result.permitted();
		}
		return evaluate(exchangeRequest, requests);
	}

	private String traceForRule(int policyIndex, int ruleIndex) {
		return String.format("rule %d.%d", policyIndex, ruleIndex);
	}

	public Trace getTrace() {
		return trace;
	}
}
