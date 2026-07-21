package com.jasminesoftwaresolutions.witchwood.application.benchmarks;

import com.jasminesoftwaresolutions.witchwood.evaluation.EvaluationEngine;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.EvaluationSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.DummySnapshotCompiler;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.DummySnapshotCompilerBuilder;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.CompiledResourcePath;
import com.jasminesoftwaresolutions.witchwood.util.JsonLogicEvaluators;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;

@State(Scope.Benchmark)
public class EvaluationEngineBenchmark {
    @Param({ "100000" })
    protected int numPrincipals;

    @Param({ "10000" })
    protected int numResources;

    @Param({ "1000" })
    protected int numPermissionSets;

    @Param({ "5" })
    protected int numPermissionSetsPerPrincipal;

    @Param({ "5" })
    protected int numServices;

    @Param({ "20" })
    protected int numActionsPerService;

    @Param({ "30" })
    protected int numStatementsPerPermissionSet;

    @Param({ "5" })
    protected int numActionsPerStatement;

    @Param({ "2" })
    protected int numResourcePatternsPerStatement;

    @Param({ "1" })
    protected int numConditionsPerStatement;

    @Param({ "2" })
    protected int numOperationsPerCondition;

    @Param({ "10" })
    protected int numVariablesPerEvaluation;

    @Param({ "3" })
    protected int numNodesPerResourcePattern;

    @Param({ "6" })
    protected int numResourceTypesPerService;

    @Param({ "4" })
    protected int statementToConditionRatio;

    @Param({ "9" })
    protected int allowToDenyEffectRatio;

    protected EvaluationSnapshot snapshot;

    protected UUID[] principals;
    protected String[] actions;
    protected String[] resources;
    protected CompiledResourcePath[] compiledResources;
    protected String[] variables;

    protected List<Map<String, Object>> contexts;

    protected EvaluationEngine engine;
    protected Random random;

    @Setup(Level.Trial)
    public void setup() {
        System.out.println("Creating dummy snapshot, this may take a while...");
        long start = System.currentTimeMillis();

        DummySnapshotCompiler compiler = new DummySnapshotCompilerBuilder()
                .setNumPrincipals(numPrincipals)
                .setNumResources(numResources)
                .setNumPermissionSets(numPermissionSets)
                .setNumPermissionSetsPerPrincipal(numPermissionSetsPerPrincipal)
                .setNumServices(numServices)
                .setNumActionsPerService(numActionsPerService)
                .setNumStatementsPerPermissionSet(numStatementsPerPermissionSet)
                .setNumActionsPerStatement(numActionsPerStatement)
                .setNumResourcePatternsPerStatement(numResourcePatternsPerStatement)
                .setNumOperationsPerCondition(numOperationsPerCondition)
                .setNumVariablesPerEvaluation(numVariablesPerEvaluation)
                .setNumNodesPerResourcePattern(numNodesPerResourcePattern)
                .setNumResourceTypesPerService(numResourceTypesPerService)
                .setStatementToConditionRatio(statementToConditionRatio)
                .setAllowToDenyEffectRatio(allowToDenyEffectRatio)
                .build().compile();

        long duration = System.currentTimeMillis() - start;
        System.out.println("Done creating snapshot in " + (duration / 1000) + " seconds.");

        snapshot = compiler.snapshot();
        principals = compiler.principals().toArray(new UUID[0]);
        resources = compiler.resources().toArray(new String[0]);
        compiledResources = compiler.compiledResources().toArray(new CompiledResourcePath[0]);
        actions = compiler.actions().toArray(new String[0]);
        variables = compiler.variables().toArray(new String[0]);

        random = new Random();

        contexts = new ArrayList<>();
        for (int i = 0; i < numVariablesPerEvaluation * 100; i++) {
            final Map<String, Object> context = new HashMap<>();
            for (int j = 0; j < numVariablesPerEvaluation; j++) {
                String key = variables[random.nextInt(variables.length)];
                int value = random.nextInt(100);
                context.put(key, value);
            }

            contexts.add(context);
        }

        engine = new EvaluationEngine(snapshot, JsonLogicEvaluators.withAllExpressions());
    }

    @Benchmark
    public void evaluateWithStringResourcePath(Blackhole bh) {
        UUID principal = principals[random.nextInt(principals.length)];
        String action = actions[random.nextInt(actions.length)];
        String resource = resources[random.nextInt(resources.length)];
        Map<String, Object> context = contexts.get(random.nextInt(contexts.size()));

        bh.consume(engine.evaluate(principal, action, resource, context));
    }

    @Benchmark
    public void evaluateWithCompiledResourcePath(Blackhole bh) {
        UUID principal = principals[random.nextInt(principals.length)];
        String action = actions[random.nextInt(actions.length)];
        CompiledResourcePath resourcePath = compiledResources[random.nextInt(resources.length)];
        Map<String, Object> context = contexts.get(random.nextInt(contexts.size()));

        bh.consume(engine.evaluate(principal, action, resourcePath, context));
    }
}