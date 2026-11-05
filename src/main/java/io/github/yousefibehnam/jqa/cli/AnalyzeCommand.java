package io.github.yousefibehnam.jqa.cli;

import io.github.yousefibehnam.jqa.build.ModelBuilder;
import io.github.yousefibehnam.jqa.metrics.ClassMetrics;
import io.github.yousefibehnam.jqa.metrics.MetricsCalculator;
import io.github.yousefibehnam.jqa.model.ProjectModel;
import io.github.yousefibehnam.jqa.parser.SourceScanner;
import io.github.yousefibehnam.jqa.parser.SourceScanner.ParseProblem;
import io.github.yousefibehnam.jqa.parser.SourceScanner.ScanResult;
import io.github.yousefibehnam.jqa.report.CsvReportWriter;
import io.github.yousefibehnam.jqa.report.JsonReportWriter;
import io.github.yousefibehnam.jqa.report.ReportWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/** {@code jqa analyze <path>}: computes metrics for every type under a source directory. */
@Command(name = "analyze",
        mixinStandardHelpOptions = true,
        description = "Compute WMC, DIT, NOC, CBO, RFC, LCOM and LCOM* for each type.")
final class AnalyzeCommand implements Callable<Integer> {

    enum Format { csv, json }

    @Parameters(index = "0", paramLabel = "PATH", description = "A source directory (e.g. src/main/java) or a .java file.")
    Path source;

    @Option(names = {"-f", "--format"}, defaultValue = "csv",
            description = "Output format: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).")
    Format format;

    @Option(names = {"-o", "--output"}, paramLabel = "FILE", description = "Write to FILE instead of standard output.")
    Path output;

    @Option(names = "--include-jdk", description = "Count couplings to JDK types (java.*, javax.*, ...) in CBO.")
    boolean includeJdk;

    @Option(names = "--fail-on-parse-error", description = "Exit with status 2 if any file cannot be parsed.")
    boolean failOnParseError;

    @Override
    public Integer call() throws IOException {
        ScanResult scan = new SourceScanner().scan(source);
        for (ParseProblem problem : scan.problems()) {
            System.err.println("warning: skipped " + problem.path() + ": " + firstLine(problem.message()));
        }
        ProjectModel model = new ModelBuilder().build(scan.files(), source);
        List<ClassMetrics> rows = new MetricsCalculator(includeJdk).calculate(model);

        ReportWriter writer = format == Format.json ? new JsonReportWriter() : new CsvReportWriter();
        if (output == null) {
            Writer out = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
            writer.write(rows, out);
        } else {
            Path parent = output.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer out = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                writer.write(rows, out);
            }
        }
        System.err.printf("analysed %d files, %d types (%d files skipped)%n",
                scan.files().size(), rows.size(), scan.problems().size());
        return failOnParseError && !scan.problems().isEmpty() ? 2 : 0;
    }

    private static String firstLine(String message) {
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
