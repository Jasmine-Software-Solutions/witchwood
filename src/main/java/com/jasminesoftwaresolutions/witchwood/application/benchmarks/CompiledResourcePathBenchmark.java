package com.jasminesoftwaresolutions.witchwood.application.benchmarks;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.DummyModelGenerator;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.DummyModelGeneratorBuilder;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.resources.ResourcePathCompiler;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;

@State(Scope.Benchmark)
public class CompiledResourcePathBenchmark {
    @Param({ "100000" })
    protected int numResources;

    @Param({ "3" })
    protected int numNodesPerResourcePattern;

    @Param({ "60" })
    protected int numResourceTypes;

    protected String[] resources;

    @Setup(Level.Trial)
    public void setup() {
        DummyModelGenerator generator = new DummyModelGeneratorBuilder()
                .setNumPrincipals(0)
                .setNumResources(numResources)
                .setNumResourceTypes(numResourceTypes)
                .setNumNodesPerResource(numNodesPerResourcePattern)
                .setNumServices(1)
                .setNumActionsPerService(1)
                .setNumHotRoles(0)
                .setNumHotDirectPolicies(0)
                .setNumRolesPerPrincipal(0)
                .setNumDirectPoliciesPerPrincipal(0)
                .setNumPoliciesPerRole(0)
                .setNumStatementsPerPolicy(0)
                .setNumActionsPerStatement(0)
                .setNumResourcesPerStatement(0)
                .setNumHotActions(0)
                .setNumHotResources(0)
                .setNumOperationsPerCondition(0)
                .setNumVariablesPerEvaluation(0)
                .build()
                .generate();

        resources = generator.resources().toArray(new String[0]);
    }

    @Benchmark
    public void compileResource(Blackhole bh) {
        bh.consume(ResourcePathCompiler.compileResource(resources[ThreadLocalRandom.current().nextInt(resources.length)]));
    }
}
