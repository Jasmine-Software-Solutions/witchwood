package com.jasminesoftwaresolutions.witchwood.application.commands.benchmark;

import com.jasminesoftwaresolutions.witchwood.application.benchmarks.ModelSnapshotCompilerBenchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.TimeUnit;

@Command(
        name = "model-snapshot-compiler",
        description = "Runs a benchmark on the model snapshot compiler with dummy generated model data",
        mixinStandardHelpOptions = true)
public class ModelSnapshotCompilerBenchmarkCommand implements Runnable {
    @Option(names = "--forks", defaultValue = "2")
    protected int forks;

    @Option(names = "--warmup-iterations", defaultValue = "5")
    protected int warmupIterations;

    @Option(names = "--warmup-time", defaultValue = "1")
    protected int warmupTime;

    @Option(names = "--measurement-iterations", defaultValue = "10")
    protected int measurementIterations;

    @Option(names = "--measurement-time", defaultValue = "1")
    protected int measurementTime;

    @Option(names = "--threads", defaultValue = "1")
    protected int threads;

    @Option(names = "--mode", defaultValue = "AverageTime")
    protected Mode mode;

    @Option(names = "--unit", defaultValue = "NANOSECONDS")
    protected TimeUnit unit;

    @Option(names = "--num-principals", defaultValue = "1000000")
    protected int numPrincipals;

    @Option(names = "--num-roles-per-principal", defaultValue = "5")
    protected int numRolesPerPrincipal;

    @Option(names = "--num-direct-policies-per-principal", defaultValue = "2")
    protected int numDirectPoliciesPerPrincipal;

    @Option(names = "--num-hot-roles", defaultValue = "100")
    protected int numHotRoles;

    @Option(names = "--num-hot-direct-policies", defaultValue = "30")
    protected int numHotDirectPolicies;

    @Option(names = "--hot-role-coverage", defaultValue = "0.7")
    protected double hotRoleCoverage;

    @Option(names = "--hot-direct-policy-coverage", defaultValue = "0.4")
    protected double hotDirectPolicyCoverage;

    @Option(names = "--num-policies-per-role", defaultValue = "3")
    protected int numPoliciesPerRole;

    @Option(names = "--num-statements-per-policy", defaultValue = "10")
    protected int numStatementsPerPolicy;

    @Option(names = "--num-actions-per-statement", defaultValue = "3")
    protected int numActionsPerStatement;

    @Option(names = "--num-resources-per-statement", defaultValue = "2")
    protected int numResourcesPerStatement;

    @Option(names = "--num-services", defaultValue = "100")
    protected int numServices;

    @Option(names = "--num-actions-per-service", defaultValue = "50")
    protected int numActionsPerService;

    @Option(names = "--num-hot-actions", defaultValue = "100")
    protected int numHotActions;

    @Option(names = "--num-hot-resources", defaultValue = "200")
    protected int numHotResources;

    @Option(names = "--allow-to-deny-effect-ratio", defaultValue = "9")
    protected int allowToDenyEffectRatio;

    @Option(names = "--condition-probability", defaultValue = "0.25")
    protected double conditionProbability;

    @Option(names = "--num-operations-per-condition", defaultValue = "2")
    protected int numOperationsPerCondition;

    @Option(names = "--num-variables-per-evaluation", defaultValue = "10")
    protected int numVariablesPerEvaluation;

    @Option(names = "--wildcard-pattern-probability", defaultValue = "0.1")
    protected double wildcardPatternProbability;

    @Option(names = "--num-resources", defaultValue = "100000")
    protected int numResources;

    @Option(names = "--num-resource-types", defaultValue = "100")
    protected int numResourceTypes;

    @Option(names = "--num-nodes-per-resource", defaultValue = "3")
    protected int numNodesPerResource;

    @Override
    public void run() {
        Options options = new OptionsBuilder()
                .include(ModelSnapshotCompilerBenchmark.class.getSimpleName())
                .forks(forks)
                .warmupIterations(warmupIterations)
                .warmupTime(TimeValue.seconds(warmupTime))
                .measurementIterations(measurementIterations)
                .measurementTime(TimeValue.seconds(measurementTime))
                .threads(threads)
                .mode(mode)
                .timeUnit(unit)
                .param("numPrincipals", String.valueOf(numPrincipals))
                .param("numRolesPerPrincipal", String.valueOf(numRolesPerPrincipal))
                .param("numDirectPoliciesPerPrincipal", String.valueOf(numDirectPoliciesPerPrincipal))
                .param("numHotRoles", String.valueOf(numHotRoles))
                .param("numHotDirectPolicies", String.valueOf(numHotDirectPolicies))
                .param("hotRoleCoverage", String.valueOf(hotRoleCoverage))
                .param("hotDirectPolicyCoverage", String.valueOf(hotDirectPolicyCoverage))
                .param("numPoliciesPerRole", String.valueOf(numPoliciesPerRole))
                .param("numStatementsPerPolicy", String.valueOf(numStatementsPerPolicy))
                .param("numActionsPerStatement", String.valueOf(numActionsPerStatement))
                .param("numResourcesPerStatement", String.valueOf(numResourcesPerStatement))
                .param("numServices", String.valueOf(numServices))
                .param("numActionsPerService", String.valueOf(numActionsPerService))
                .param("numHotActions", String.valueOf(numHotActions))
                .param("numHotResources", String.valueOf(numHotResources))
                .param("allowToDenyEffectRatio", String.valueOf(allowToDenyEffectRatio))
                .param("conditionProbability", String.valueOf(conditionProbability))
                .param("numOperationsPerCondition", String.valueOf(numOperationsPerCondition))
                .param("numVariablesPerEvaluation", String.valueOf(numVariablesPerEvaluation))
                .param("wildcardPatternProbability", String.valueOf(wildcardPatternProbability))
                .param("numResources", String.valueOf(numResources))
                .param("numResourceTypes", String.valueOf(numResourceTypes))
                .param("numNodesPerResource", String.valueOf(numNodesPerResource))
                .build();

        try {
            new Runner(options).run();
        } catch (RunnerException e) {
            throw new RuntimeException(e);
        }
    }
}
