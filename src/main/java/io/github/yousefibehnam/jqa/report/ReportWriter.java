package io.github.yousefibehnam.jqa.report;

import io.github.yousefibehnam.jqa.metrics.ClassMetrics;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/** Writes metric rows in one output format. */
public interface ReportWriter {

    List<String> COLUMNS = List.of(
            "class", "kind", "file", "methods", "fields",
            "wmc", "dit", "dit_exact", "noc", "cbo", "rfc", "lcom", "lcom_star");

    void write(List<ClassMetrics> rows, Writer out) throws IOException;

    /** LCOM* with four decimals and a dot separator regardless of the default locale. */
    static String formatRatio(OptionalDouble value) {
        return value.isPresent() ? String.format(Locale.ROOT, "%.4f", value.getAsDouble()) : null;
    }
}
