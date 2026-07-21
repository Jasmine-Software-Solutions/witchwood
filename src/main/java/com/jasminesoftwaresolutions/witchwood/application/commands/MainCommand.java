package com.jasminesoftwaresolutions.witchwood.application.commands;

import com.jasminesoftwaresolutions.witchwood.application.commands.benchmark.BenchmarkCommand;
import picocli.CommandLine.Command;

@Command(
    name = "witchwood",
    mixinStandardHelpOptions = true,
    subcommands = {
        BenchmarkCommand.class
    })
public class MainCommand implements Runnable {
    @Override
    public void run() {}
}