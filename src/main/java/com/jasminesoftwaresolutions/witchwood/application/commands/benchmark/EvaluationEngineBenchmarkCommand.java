package com.jasminesoftwaresolutions.witchwood.application.commands.benchmark;

import com.jasminesoftwaresolutions.witchwood.application.benchmarks.EvaluationEngineBenchmark;
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
        name = "evaluation-engine",
        description = "Runs a benchmark on the evaluation engine with dummy data",
        mixinStandardHelpOptions = true)
public class EvaluationEngineBenchmarkCommand implements Runnable {
    @Option(names = "--fork", defaultValue = "2")
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

    @Option(names = "--num-principals", defaultValue = "100000")
    protected int numPrincipals;

    @Option(names = "--num-resources", defaultValue = "10000")
    protected int numResources;

    @Option(names = "--num-permission-sets", defaultValue = "1000")
    protected int numPermissionSets;

    @Option(names = "--num-permission-sets-per-principal", defaultValue = "5")
    protected int numPermissionSetsPerPrincipal;

    @Option(names = "--num-services", defaultValue = "5")
    protected int numServices;

    @Option(names = "--num-actions-per-service", defaultValue = "20")
    protected int numActionsPerService;

    @Option(names = "--num-statements-per-permission-set", defaultValue = "30")
    protected int numStatementsPerPermissionSet;

    @Option(names = "--num-actions-per-statement", defaultValue = "5")
    protected int numActionsPerStatement;

    @Option(names = "--num-resource-patterns-per-statement", defaultValue = "2")
    protected int numResourcePatternsPerStatement;

    @Option(names = "--num-conditions-per-statement", defaultValue = "1")
    protected int numConditionsPerStatement;

    @Option(names = "--num-operations-per-condition", defaultValue = "2")
    protected int numOperationsPerCondition;

    @Option(names = "--num-variables-per-evaluation", defaultValue = "10")
    protected int numVariablesPerEvaluation;

    @Option(names = "--num-nodes-per-resource-pattern", defaultValue = "3")
    protected int numNodesPerResourcePattern;

    @Option(names = "--num-resource-types-per-service", defaultValue = "6")
    protected int numResourceTypesPerService;

    @Option(names = "--statement-to-condition-ratio", defaultValue = "4")
    protected int statementToConditionRatio;

    @Option(names = "--allow-to-deny-effect-ratio", defaultValue = "9")
    protected int allowToDenyEffectRatio;

    @Override
    public void run() {
        Options options = new OptionsBuilder()
                .include(EvaluationEngineBenchmark.class.getSimpleName())
                .forks(forks)
                .warmupIterations(warmupIterations)
                .warmupTime(TimeValue.seconds(warmupTime))
                .measurementIterations(measurementIterations)
                .measurementTime(TimeValue.seconds(measurementTime))
                .threads(threads)
                .mode(mode)
                .timeUnit(unit)
                .param("numPrincipals", String.valueOf(numPrincipals))
                .param("numResources", String.valueOf(numResources))
                .param("numPermissionSets", String.valueOf(numPermissionSets))
                .param("numPermissionSetsPerPrincipal", String.valueOf(numPermissionSetsPerPrincipal))
                .param("numServices", String.valueOf(numServices))
                .param("numActionsPerService", String.valueOf(numActionsPerService))
                .param("numStatementsPerPermissionSet", String.valueOf(numStatementsPerPermissionSet))
                .param("numActionsPerStatement", String.valueOf(numActionsPerStatement))
                .param("numResourcePatternsPerStatement", String.valueOf(numResourcePatternsPerStatement))
                .param("numConditionsPerStatement", String.valueOf(numConditionsPerStatement))
                .param("numOperationsPerCondition", String.valueOf(numOperationsPerCondition))
                .param("numVariablesPerEvaluation", String.valueOf(numVariablesPerEvaluation))
                .param("numNodesPerResourcePattern", String.valueOf(numNodesPerResourcePattern))
                .param("numResourceTypesPerService", String.valueOf(numResourceTypesPerService))
                .param("statementToConditionRatio", String.valueOf(statementToConditionRatio))
                .param("allowToDenyEffectRatio", String.valueOf(allowToDenyEffectRatio))
                .build();

        try {
            new Runner(options).run();
        } catch (RunnerException e) {
            throw new RuntimeException(e);
        }
    }
}