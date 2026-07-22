package com.jasminesoftwaresolutions.witchwood.application.commands.benchmark;

import com.jasminesoftwaresolutions.witchwood.application.benchmarks.PersistedSnapshotCompilerBenchmark;
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
        name = "persisted-snapshot-compiler",
        description = "Benchmarks snapshot compilation streaming from a PostgreSQL database",
        mixinStandardHelpOptions = true)
public class PersistedSnapshotCompilerBenchmarkCommand implements Runnable {
    @Option(names = "--forks", defaultValue = "1")
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

    @Option(names = "--unit", defaultValue = "SECONDS")
    protected TimeUnit unit;

    @Option(names = "--jdbc-url", defaultValue = "jdbc:postgresql://localhost:5432/witchwood")
    protected String jdbcUrl;

    @Option(names = "--jdbc-user", defaultValue = "witchwood")
    protected String jdbcUser;

    @Option(names = "--jdbc-password", defaultValue = "secret")
    protected String jdbcPassword;

    @Override
    public void run() {
        Options options = new OptionsBuilder()
                .include(PersistedSnapshotCompilerBenchmark.class.getSimpleName())
                .forks(forks)
                .warmupIterations(warmupIterations)
                .warmupTime(TimeValue.seconds(warmupTime))
                .measurementIterations(measurementIterations)
                .measurementTime(TimeValue.seconds(measurementTime))
                .threads(threads)
                .mode(mode)
                .timeUnit(unit)
                .param("jdbcUrl", jdbcUrl)
                .param("jdbcUser", jdbcUser)
                .param("jdbcPassword", jdbcPassword)
                .build();

        try {
            new Runner(options).run();
        } catch (RunnerException e) {
            throw new RuntimeException(e);
        }
    }
}
