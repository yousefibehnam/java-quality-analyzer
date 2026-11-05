package io.github.yousefibehnam.jqa.summary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads the CSV written by {@code jqa analyze} back into rows of named cells. */
public final class MetricsCsvReader {

    private MetricsCsvReader() {
    }

    public static List<Map<String, String>> read(Path file) throws IOException {
        List<String> records = splitRecords(Files.readString(file, StandardCharsets.UTF_8));
        if (records.isEmpty()) {
            throw new IOException("empty CSV: " + file);
        }
        List<String> header = parseRecord(records.get(0));
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> cells = parseRecord(records.get(i));
            if (cells.size() != header.size()) {
                throw new IOException(file + ": record " + i + " has " + cells.size()
                        + " cells, expected " + header.size());
            }
            Map<String, String> row = new HashMap<>();
            for (int c = 0; c < header.size(); c++) {
                row.put(header.get(c), cells.get(c));
            }
            rows.add(row);
        }
        return rows;
    }

    /** Splits text into records, keeping line breaks that are inside quotes. */
    static List<String> splitRecords(String text) {
        List<String> records = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            }
            if ((c == '\n' || c == '\r') && !quoted) {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                if (current.length() > 0) {
                    records.add(current.toString());
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            records.add(current.toString());
        }
        return records;
    }

    static List<String> parseRecord(String record) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < record.length(); i++) {
            char c = record.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < record.length() && record.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString());
        return cells;
    }
}
