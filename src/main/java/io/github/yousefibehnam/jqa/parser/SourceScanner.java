package io.github.yousefibehnam.jqa.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Finds {@code .java} files under a directory and parses them.
 *
 * <p>Files that fail to parse are reported instead of aborting the whole run, so a
 * single unsupported construct does not hide the results for the rest of a project.
 */
public final class SourceScanner {

    /** A successfully parsed source file. */
    public record ParsedFile(Path path, CompilationUnit unit) {
    }

    /** A source file that could not be parsed, with the first parser message. */
    public record ParseProblem(Path path, String message) {
    }

    /** The outcome of scanning one source tree. */
    public record ScanResult(List<ParsedFile> files, List<ParseProblem> problems) {
    }

    private final JavaParser parser;

    public SourceScanner() {
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(LanguageLevel.JAVA_21)
                .setAttributeComments(false);
        this.parser = new JavaParser(configuration);
    }

    public ScanResult scan(Path root) {
        if (!Files.exists(root)) {
            throw new IllegalArgumentException("path does not exist: " + root);
        }
        List<ParsedFile> parsed = new ArrayList<>();
        List<ParseProblem> problems = new ArrayList<>();
        for (Path file : findSourceFiles(root)) {
            try {
                ParseResult<CompilationUnit> result = parser.parse(file);
                if (result.isSuccessful() && result.getResult().isPresent()) {
                    parsed.add(new ParsedFile(file, result.getResult().get()));
                } else {
                    String message = result.getProblems().isEmpty()
                            ? "unknown parse error"
                            : result.getProblem(0).getVerboseMessage();
                    problems.add(new ParseProblem(file, message));
                }
            } catch (IOException e) {
                problems.add(new ParseProblem(file, e.getMessage()));
            }
        }
        return new ScanResult(parsed, problems);
    }

    /** Regular {@code .java} files in a stable order, skipping module and package descriptors. */
    static List<Path> findSourceFiles(Path root) {
        if (Files.isRegularFile(root)) {
            return isTypeSource(root) ? List.of(root) : List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(SourceScanner::isTypeSource)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isTypeSource(Path file) {
        String name = file.getFileName().toString();
        return name.endsWith(".java")
                && !name.equals("module-info.java")
                && !name.equals("package-info.java");
    }
}
