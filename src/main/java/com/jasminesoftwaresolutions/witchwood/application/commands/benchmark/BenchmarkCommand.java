package com.jasminesoftwaresolutions.witchwood.application.commands.benchmark;

import picocli.CommandLine.Command;

@Command(
        name = "benchmark",
        mixinStandardHelpOptions = true,
        subcommands = {
                CompiledResourcePathBenchmarkCommand.class,
                EvaluationEngineBenchmarkCommand.class,
                ModelSnapshotCompilerBenchmarkCommand.class,
                PersistedSnapshotCompilerBenchmarkCommand.class
        })
public class BenchmarkCommand implements Runnable {
    @Override
    public void run() {}
}