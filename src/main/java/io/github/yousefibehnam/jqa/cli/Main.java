package io.github.yousefibehnam.jqa.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/** Entry point of the {@code jqa} command-line tool. */
@Command(name = "jqa",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class,
        description = "Java Quality Analyzer: Chidamber-Kemerer metrics for Java source code.",
        subcommands = {AnalyzeCommand.class, SummarizeCommand.class})
public final class Main implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /** Runs the tool without exiting the JVM; returns the exit code. */
    public static int run(String... args) {
        return new CommandLine(new Main()).execute(args);
    }

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}
