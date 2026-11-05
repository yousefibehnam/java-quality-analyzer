package io.github.yousefibehnam.jqa.cli;

import io.github.yousefibehnam.jqa.metrics.ResolutionStats;
import io.github.yousefibehnam.jqa.summary.MetricsCsvReader;
import io.github.yousefibehnam.jqa.summary.SummaryReport;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

/** {@code jqa summarize name=metrics.csv ...}: a Markdown summary of several analysed projects. */
@Command(name = "summarize",
        mixinStandardHelpOptions = true,
        description = {"Summarise CSV files written by 'jqa analyze' as Markdown.",
            "If NAME.stats.properties (from --stats) exists next to a CSV file, "
                + "its resolution rates are included."})
final class SummarizeCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(arity = "1..*", paramLabel = "NAME=CSV", description = "Project name and its metrics CSV.")
    List<String> inputs;

    @Option(names = {"-o", "--output"}, paramLabel = "FILE", description = "Write to FILE instead of standard output.")
    Path output;

    @Option(names = "--note", paramLabel = "TEXT", description = "A line to list under the title (repeatable).")
    List<String> notes = new ArrayList<>();

    @Override
    public Integer call() throws IOException {
        List<SummaryReport.Project> projects = new ArrayList<>();
        for (String input : inputs) {
            int eq = input.indexOf('=');
            if (eq <= 0) {
                throw new CommandLine.ParameterException(spec.commandLine(), "expected NAME=CSV but got: " + input);
            }
            Path csv = Path.of(input.substring(eq + 1));
            projects.add(new SummaryReport.Project(input.substring(0, eq), MetricsCsvReader.read(csv), stats(csv)));
        }
        String markdown = SummaryReport.render(projects, notes);
        if (output == null) {
            System.out.print(markdown);
        } else {
            Files.writeString(output, markdown, StandardCharsets.UTF_8);
        }
        return 0;
    }

    static Path statsPathFor(Path csv) {
        String name = csv.getFileName().toString();
        String base = name.endsWith(".csv") ? name.substring(0, name.length() - 4) : name;
        return csv.resolveSibling(base + ".stats.properties");
    }

    private static ResolutionStats stats(Path csv) throws IOException {
        Path file = statsPathFor(csv);
        if (!Files.exists(file)) {
            return null;
        }
        Properties p = new Properties();
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(in);
        }
        return ResolutionStats.fromProperties(p);
    }
}
