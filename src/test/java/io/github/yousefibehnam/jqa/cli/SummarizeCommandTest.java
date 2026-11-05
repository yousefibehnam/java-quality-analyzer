package io.github.yousefibehnam.jqa.cli;

import io.github.yousefibehnam.jqa.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SummarizeCommandTest {

    @TempDir
    Path dir;

    @Test
    void summarisesSeveralProjects() throws IOException {
        Path cohesion = dir.resolve("cohesion.csv");
        Path coupling = dir.resolve("coupling.csv");
        assertThat(Main.run("analyze", Fixtures.path("cohesion").toString(), "-o", cohesion.toString(),
                "--stats", dir.resolve("cohesion.stats.properties").toString())).isZero();
        assertThat(Main.run("analyze", Fixtures.path("coupling").toString(), "-o", coupling.toString())).isZero();

        Path summary = dir.resolve("summary.md");
        int code = Main.run("summarize", "-o", summary.toString(), "--note", "fixtures only",
                "cohesion=" + cohesion, "coupling=" + coupling);

        assertThat(code).isZero();
        String md = Files.readString(summary);
        assertThat(md).contains("# Case-study results", "- fixtures only", "## Overview",
                "## cohesion", "## coupling", "## Largest classes by WMC", "## Spearman rank correlation");
        // cohesion has stats (4 classes, all call sites resolvable); coupling has none.
        assertThat(md).contains("| cohesion | 3 | 4 | 4 | 0 | 100.0% | 100.0% |");
        assertThat(md).contains("| coupling | n/a | 9 | 9 | 0 | n/a | n/a |");
        // coupling CBO values 4,1,2,1,1,1,2,2,2: median 2, max 4
        assertThat(md).contains("| CBO | 9 | 1.78 | 2.0 | 2.4 | 4 |");
    }

    @Test
    void rejectsInputWithoutName() {
        assertThat(Main.run("summarize", "metrics.csv")).isEqualTo(2); // picocli usage error
    }

    @Test
    void statsFileSitsNextToCsv() {
        assertThat(SummarizeCommand.statsPathFor(Path.of("results/x.csv")))
                .isEqualTo(Path.of("results/x.stats.properties"));
    }
}
