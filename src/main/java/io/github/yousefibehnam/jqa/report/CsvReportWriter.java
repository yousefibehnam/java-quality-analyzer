package io.github.yousefibehnam.jqa.report;

import io.github.yousefibehnam.jqa.metrics.ClassMetrics;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/** RFC 4180 CSV with a header row; an undefined LCOM* is an empty cell. */
public final class CsvReportWriter implements ReportWriter {

    @Override
    public void write(List<ClassMetrics> rows, Writer out) throws IOException {
        out.write(String.join(",", COLUMNS));
        out.write("\n");
        for (ClassMetrics r : rows) {
            String lcomStar = ReportWriter.formatRatio(r.lcomStar());
            out.write(String.join(",",
                    escape(r.className()), r.kind().name().toLowerCase(java.util.Locale.ROOT), escape(r.sourceFile()),
                    Integer.toString(r.methods()), Integer.toString(r.fields()),
                    Integer.toString(r.wmc()), Integer.toString(r.dit()), Boolean.toString(r.ditExact()),
                    Integer.toString(r.noc()), Integer.toString(r.cbo()), Integer.toString(r.rfc()),
                    Integer.toString(r.lcom()), lcomStar == null ? "" : lcomStar));
            out.write("\n");
        }
        out.flush();
    }

    static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
