package com.jasminesoftwaresolutions.witchwood.application;

import com.jasminesoftwaresolutions.witchwood.application.commands.MainCommand;
import picocli.CommandLine;

public class Application {
    static void main(String[] args) {
        int exitCode = new CommandLine(new MainCommand())
                .execute(args);

        System.exit(exitCode);
    }
}
