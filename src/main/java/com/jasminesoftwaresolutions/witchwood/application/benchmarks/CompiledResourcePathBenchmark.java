package com.jasminesoftwaresolutions.witchwood.application.benchmarks;

import com.jasminesoftwaresolutions.witchwood.evaluation.EvaluationEngine;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.EvaluationSnapshot;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.DummySnapshotCompiler;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.DummySnapshotCompilerBuilder;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.ResourcePathCompiler;
import com.jasminesoftwaresolutions.witchwood.util.JsonLogicEvaluators;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;

@State(Scope.Benchmark)
public class CompiledResourcePathBenchmark {
    @Param({ "100000" })
    protected int numResources;

    @Param({ "3" })
    protected int numNodesPerResourcePattern;

    @Param({ "60" })
    protected int numResourceTypes;

    protected EvaluationSnapshot snapshot;
    protected String[] resources;

    protected EvaluationEngine engine;
    protected Random random;

    @Setup(Level.Trial)
    public void setup() {
        System.out.println("Creating dummy snapshot, this may take a while...");
        long start = System.currentTimeMillis();

        DummySnapshotCompiler compiler = new DummySnapshotCompilerBuilder()
                .setNumPrincipals(0)
                .setNumResources(numResources)
                .setNumPermissionSets(0)
                .setNumPermissionSetsPerPrincipal(0)
                .setNumServices(1)
                .setNumActionsPerService(60)
                .setNumStatementsPerPermissionSet(0)
                .setNumActionsPerStatement(0)
                .setNumResourcePatternsPerStatement(0)
                .setNumOperationsPerCondition(0)
                .setNumVariablesPerEvaluation(0)
                .setNumNodesPerResourcePattern(numNodesPerResourcePattern)
                .setNumResourceTypesPerService(numResourceTypes)
                .setStatementToConditionRatio(0)
                .setAllowToDenyEffectRatio(0)
                .build().compile();

        long duration = System.currentTimeMillis() - start;
        System.out.println("Done creating snapshot in " + (duration / 1000) + " seconds.");

        random = new Random();
        resources = compiler.resources().toArray(new String[0]);
        engine = new EvaluationEngine(snapshot, JsonLogicEvaluators.withAllExpressions());
    }

    @Benchmark
    public void compileResource(Blackhole bh) {
        bh.consume(ResourcePathCompiler.compileResource(resources[random.nextInt(resources.length)]));
    }
}