package io.github.yousefibehnam.jqa;

import io.github.yousefibehnam.jqa.build.ModelBuilder;
import io.github.yousefibehnam.jqa.metrics.ClassMetrics;
import io.github.yousefibehnam.jqa.metrics.MetricsCalculator;
import io.github.yousefibehnam.jqa.model.ProjectModel;
import io.github.yousefibehnam.jqa.parser.SourceScanner;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads the hand-written Java fixtures under {@code src/test/resources/fixtures}. */
public final class Fixtures {

    private Fixtures() {
    }

    public static Path path(String name) {
        URL url = Fixtures.class.getResource("/fixtures/" + name);
        if (url == null) {
            throw new IllegalArgumentException("no fixture directory " + name);
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ProjectModel model(String name) {
        Path root = path(name);
        SourceScanner.ScanResult scan = new SourceScanner().scan(root);
        if (!scan.problems().isEmpty()) {
            throw new IllegalStateException("fixture does not parse: " + scan.problems());
        }
        return new ModelBuilder().build(scan.files(), root);
    }

    /** Metrics of every type in the fixture, keyed by qualified class name. */
    public static Map<String, ClassMetrics> metrics(String name) {
        return metrics(name, false);
    }

    public static Map<String, ClassMetrics> metrics(String name, boolean includeJdk) {
        Map<String, ClassMetrics> byName = new LinkedHashMap<>();
        for (ClassMetrics row : new MetricsCalculator(includeJdk).calculate(model(name))) {
            byName.put(row.className(), row);
        }
        return byName;
    }
}
