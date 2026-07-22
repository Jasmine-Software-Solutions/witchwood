package com.jasminesoftwaresolutions.witchwood.application.benchmarks;

import com.jasminesoftwaresolutions.witchwood.evaluation.EvaluationEngine;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.EvaluationSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.DummyModelGenerator;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.DummyModelGeneratorBuilder;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.ModelSnapshotCompiler;
import com.jasminesoftwaresolutions.witchwood.util.JsonLogicEvaluators;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@State(Scope.Benchmark)
public class EvaluationEngineBenchmark {
    @Param({ "1000000" })
    protected int numPrincipals;

    @Param({ "5" })
    protected int numRolesPerPrincipal;

    @Param({ "2" })
    protected int numDirectPoliciesPerPrincipal;

    @Param({ "100" })
    protected int numHotRoles;

    @Param({ "30" })
    protected int numHotDirectPolicies;

    @Param({ "0.7" })
    protected double hotRoleCoverage;

    @Param({ "0.4" })
    protected double hotDirectPolicyCoverage;

    @Param({ "3" })
    protected int numPoliciesPerRole;

    @Param({ "10" })
    protected int numStatementsPerPolicy;

    @Param({ "3" })
    protected int numActionsPerStatement;

    @Param({ "2" })
    protected int numResourcesPerStatement;

    @Param({ "100" })
    protected int numServices;

    @Param({ "50" })
    protected int numActionsPerService;

    @Param({ "100" })
    protected int numHotActions;

    @Param({ "200" })
    protected int numHotResources;

    @Param({ "9" })
    protected int allowToDenyEffectRatio;

    @Param({ "0.25" })
    protected double conditionProbability;

    @Param({ "2" })
    protected int numOperationsPerCondition;

    @Param({ "10" })
    protected int numVariablesPerEvaluation;

    @Param({ "0.1" })
    protected double wildcardPatternProbability;

    @Param({ "100000" })
    protected int numResources;

    @Param({ "100" })
    protected int numResourceTypes;

    @Param({ "3" })
    protected int numNodesPerResource;

    @Param({ "0.7" })
    protected double allowQueryProbability;

    @Param({ "0.2" })
    protected double explicitDenyQueryProbability;

    protected EvaluationSnapshot snapshot;
    protected EvaluationEngine engine;

    protected String[] variables;
    protected List<Map<String, Object>> contexts;

    protected DummyModelGenerator.QueryPools.Query[] allowQueries;
    protected DummyModelGenerator.QueryPools.Query[] explicitDenyQueries;
    protected DummyModelGenerator.QueryPools.Query[] defaultDenyQueries;

    @Setup(Level.Trial)
    public void setup() {
        DummyModelGeneratorBuilder builder = new DummyModelGeneratorBuilder()
                .setNumPrincipals(numPrincipals)
                .setNumRolesPerPrincipal(numRolesPerPrincipal)
                .setNumDirectPoliciesPerPrincipal(numDirectPoliciesPerPrincipal)
                .setNumHotRoles(numHotRoles)
                .setNumHotDirectPolicies(numHotDirectPolicies)
                .setHotRoleCoverage(hotRoleCoverage)
                .setHotDirectPolicyCoverage(hotDirectPolicyCoverage)
                .setNumPoliciesPerRole(numPoliciesPerRole)
                .setNumStatementsPerPolicy(numStatementsPerPolicy)
                .setNumActionsPerStatement(numActionsPerStatement)
                .setNumResourcesPerStatement(numResourcesPerStatement)
                .setNumServices(numServices)
                .setNumActionsPerService(numActionsPerService)
                .setNumHotActions(numHotActions)
                .setNumHotResources(numHotResources)
                .setAllowToDenyEffectRatio(allowToDenyEffectRatio)
                .setConditionProbability(conditionProbability)
                .setNumOperationsPerCondition(numOperationsPerCondition)
                .setNumVariablesPerEvaluation(numVariablesPerEvaluation)
                .setWildcardPatternProbability(wildcardPatternProbability)
                .setNumResources(numResources)
                .setNumResourceTypes(numResourceTypes)
                .setNumNodesPerResource(numNodesPerResource);

        DummyModelGenerator generator = builder.build()
                .generate();

        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(
                generator.serviceDirectory(),
                generator.principalObjects()
        ).compile();

        snapshot = compiler.snapshot();
        variables = generator.variables().toArray(new String[0]);

        // Pre-compute contexts
        contexts = new ArrayList<>();
        for (int i = 0; i < numVariablesPerEvaluation * 100; i++) {
            final Map<String, Object> context = new HashMap<>();
            for (int j = 0; j < numVariablesPerEvaluation; j++) {
                String key = variables[ThreadLocalRandom.current().nextInt(variables.length)];
                int value = ThreadLocalRandom.current().nextInt(100);
                context.put(key, value);
            }
            contexts.add(context);
        }

        // Load query pools
        DummyModelGenerator.QueryPools pools = generator.queryPools();
        allowQueries = pools.allow().toArray(new DummyModelGenerator.QueryPools.Query[0]);
        explicitDenyQueries = pools.explicitDeny().toArray(new DummyModelGenerator.QueryPools.Query[0]);
        defaultDenyQueries = pools.defaultDeny().toArray(new DummyModelGenerator.QueryPools.Query[0]);

        System.out.println("Query pools: " + allowQueries.length + " allow, "
                + explicitDenyQueries.length + " explicit deny, "
                + defaultDenyQueries.length + " default deny");

        engine = new EvaluationEngine(snapshot, JsonLogicEvaluators.withAllExpressions());
    }

    @Benchmark
    public void evaluate(Blackhole bh) {
        var query = nextQuery();
        Map<String, Object> context = contexts.get(ThreadLocalRandom.current().nextInt(contexts.size()));
        bh.consume(engine.evaluate(query.principal(), query.action(), query.resource(), context));
    }

    /**
     * Pick the next query from the pools according to the configured distribution.
     * Distribution: allowQueryProbability → ALLOW,
     *               explicitDenyQueryProbability → explicit DENY,
     *               remainder → default DENY.
     */
    protected DummyModelGenerator.QueryPools.Query nextQuery() {
        double r = ThreadLocalRandom.current().nextDouble();
        DummyModelGenerator.QueryPools.Query[] pool;

        if (r < allowQueryProbability && allowQueries.length > 0) {
            pool = allowQueries;
        } else if (r < allowQueryProbability + explicitDenyQueryProbability && explicitDenyQueries.length > 0) {
            pool = explicitDenyQueries;
        } else if (defaultDenyQueries.length > 0) {
            pool = defaultDenyQueries;
        } else {
            // Fallback: use the largest pool
            pool = allowQueries.length > 0 ? allowQueries
                    : explicitDenyQueries.length > 0 ? explicitDenyQueries
                    : defaultDenyQueries;
        }

        return pool[ThreadLocalRandom.current().nextInt(pool.length)];
    }
}
