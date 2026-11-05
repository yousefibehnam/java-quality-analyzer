package io.github.yousefibehnam.jqa.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceScannerTest {

    @TempDir
    Path dir;

    @Test
    void parsesValidFilesAndReportsBrokenOnes() throws IOException {
        Files.createDirectories(dir.resolve("a"));
        Files.writeString(dir.resolve("a/Good.java"), "package a; class Good {}");
        Files.writeString(dir.resolve("a/Bad.java"), "package a; class Bad { void m( }");
        Files.writeString(dir.resolve("a/package-info.java"), "package a;");
        Files.writeString(dir.resolve("module-info.java"), "module a {}");
        Files.writeString(dir.resolve("notes.txt"), "not java");

        SourceScanner.ScanResult result = new SourceScanner().scan(dir);

        assertThat(result.files()).extracting(f -> f.path().getFileName().toString()).containsExactly("Good.java");
        assertThat(result.problems()).extracting(p -> p.path().getFileName().toString()).containsExactly("Bad.java");
    }

    @Test
    void acceptsASingleFile() throws IOException {
        Path file = Files.writeString(dir.resolve("One.java"), "class One {}");
        assertThat(new SourceScanner().scan(file).files()).hasSize(1);
    }

    @Test
    void rejectsMissingPath() {
        assertThatThrownBy(() -> new SourceScanner().scan(dir.resolve("missing")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
