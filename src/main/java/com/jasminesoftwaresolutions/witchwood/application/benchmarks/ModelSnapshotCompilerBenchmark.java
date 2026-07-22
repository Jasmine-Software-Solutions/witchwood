package com.jasminesoftwaresolutions.witchwood.application.benchmarks;

import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.ServiceDirectory;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.DummyModelGenerator;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.DummyModelGeneratorBuilder;
import com.jasminesoftwaresolutions.witchwood.evaluation.snapshot.compilers.ModelSnapshotCompiler;
import com.jasminesoftwaresolutions.witchwood.models.IPrincipal;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;

@State(Scope.Benchmark)
public class ModelSnapshotCompilerBenchmark {
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

    protected ServiceDirectory serviceDirectory;
    protected List<IPrincipal> principals;

    @Setup(Level.Trial)
    public void setup() {
        DummyModelGenerator generator = new DummyModelGeneratorBuilder()
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
                .setNumNodesPerResource(numNodesPerResource)
                .build()
                .generate();

        serviceDirectory = generator.serviceDirectory();
        principals = generator.principalObjects();
    }

    @Benchmark
    public void compile(Blackhole bh) {
        ModelSnapshotCompiler compiler = new ModelSnapshotCompiler(serviceDirectory, principals)
                .compile();

        bh.consume(compiler.snapshot());
    }
}
