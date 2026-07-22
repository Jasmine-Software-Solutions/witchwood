package com.jasminesoftwaresolutions.witchwood.application.commands.benchmark;

import com.jasminesoftwaresolutions.witchwood.application.benchmarks.CompiledResourcePathBenchmark;
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
        name = "compiled-resource-path",
        description = "Runs a benchmark on the resource path compiler with dummy data",
        mixinStandardHelpOptions = true)
public class CompiledResourcePathBenchmarkCommand implements Runnable {
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

    @Option(names = "--num-resources", defaultValue = "100000")
    protected int numResources;

    @Option(names = "--num-nodes-per-resource-pattern", defaultValue = "3")
    protected int numNodesPerResourcePattern;

    @Option(names = "--num-resource-types", defaultValue = "60")
    protected int numResourceTypes;

    @Override
    public void run() {
        Options options = new OptionsBuilder()
                .include(CompiledResourcePathBenchmark.class.getSimpleName())
                .forks(forks)
                .warmupIterations(warmupIterations)
                .warmupTime(TimeValue.seconds(warmupTime))
                .measurementIterations(measurementIterations)
                .measurementTime(TimeValue.seconds(measurementTime))
                .threads(threads)
                .mode(mode)
                .timeUnit(unit)
                .param("numResources", String.valueOf(numResources))
                .param("numNodesPerResourcePattern", String.valueOf(numNodesPerResourcePattern))
                .param("numResourceTypes", String.valueOf(numResourceTypes))
                .build();

        try {
            new Runner(options).run();
        } catch (RunnerException e) {
            throw new RuntimeException(e);
        }
    }
}