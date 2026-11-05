package io.github.yousefibehnam.jqa.cli;

import io.github.yousefibehnam.jqa.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    @TempDir
    Path dir;

    @Test
    void analyzeWritesCsvFile() throws IOException {
        Path out = dir.resolve("out/metrics.csv");
        int code = Main.run("analyze", Fixtures.path("cohesion").toString(), "-o", out.toString());

        assertThat(code).isZero();
        List<String> lines = Files.readAllLines(out);
        assertThat(lines.get(0)).startsWith("class,kind,file,");
        assertThat(lines).anyMatch(l -> l.startsWith("fx.lcom.Counter,class,fx/lcom/Counter.java,4,3,"));
    }

    @Test
    void analyzeWritesJson() throws IOException {
        Path out = dir.resolve("metrics.json");
        int code = Main.run("analyze", Fixtures.path("cohesion").toString(), "--format", "json", "-o", out.toString());

        assertThat(code).isZero();
        assertThat(Files.readString(out)).contains("\"class\": \"fx.lcom.Vector2\"", "\"lcom_star\": 0.2500");
    }

    @Test
    void statsFileRecordsResolutionCounts() throws IOException {
        Path out = dir.resolve("m.csv");
        Path stats = dir.resolve("m.properties");
        int code = Main.run("analyze", Fixtures.path("response").toString(), "-o", out.toString(),
                "--stats", stats.toString());

        assertThat(code).isZero();
        java.util.Properties p = new java.util.Properties();
        try (var in = Files.newBufferedReader(stats)) {
            p.load(in);
        }
        // Account: field initializer 1, this(0) 1, deposit 2, withdraw 3, check 1, describe 2 = 10
        // call sites, all resolvable; Ledger makes none.
        assertThat(p).containsEntry("types", "2").containsEntry("call_sites", "10")
                .containsEntry("resolved_call_sites", "10");
    }

    @Test
    void parseErrorsFailOnlyWhenRequested() throws IOException {
        Files.writeString(dir.resolve("Bad.java"), "class Bad {");
        Path out = dir.resolve("m.csv");
        assertThat(Main.run("analyze", dir.toString(), "-o", out.toString())).isZero();
        assertThat(Main.run("analyze", dir.toString(), "-o", out.toString(), "--fail-on-parse-error")).isEqualTo(2);
    }

    @Test
    void unknownOptionIsAUsageError() {
        assertThat(Main.run("analyze", "--nope")).isEqualTo(2);
    }
}
