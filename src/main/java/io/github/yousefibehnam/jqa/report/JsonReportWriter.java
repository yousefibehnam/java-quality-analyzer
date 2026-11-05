package io.github.yousefibehnam.jqa.report;

import io.github.yousefibehnam.jqa.metrics.ClassMetrics;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;

/** A JSON array with one object per class; an undefined LCOM* is {@code null}. */
public final class JsonReportWriter implements ReportWriter {

    @Override
    public void write(List<ClassMetrics> rows, Writer out) throws IOException {
        out.write("[");
        for (int i = 0; i < rows.size(); i++) {
            ClassMetrics r = rows.get(i);
            String lcomStar = ReportWriter.formatRatio(r.lcomStar());
            out.write(i == 0 ? "\n" : ",\n");
            out.write("  {");
            out.write("\"class\": " + quote(r.className()));
            out.write(", \"kind\": " + quote(r.kind().name().toLowerCase(Locale.ROOT)));
            out.write(", \"file\": " + quote(r.sourceFile()));
            out.write(", \"methods\": " + r.methods());
            out.write(", \"fields\": " + r.fields());
            out.write(", \"wmc\": " + r.wmc());
            out.write(", \"dit\": " + r.dit());
            out.write(", \"dit_exact\": " + r.ditExact());
            out.write(", \"noc\": " + r.noc());
            out.write(", \"cbo\": " + r.cbo());
            out.write(", \"rfc\": " + r.rfc());
            out.write(", \"lcom\": " + r.lcom());
            out.write(", \"lcom_star\": " + (lcomStar == null ? "null" : lcomStar));
            out.write("}");
        }
        out.write(rows.isEmpty() ? "]\n" : "\n]\n");
        out.flush();
    }

    static String quote(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
