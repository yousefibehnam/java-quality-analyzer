package io.github.yousefibehnam.jqa.summary;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StatisticsTest {

    @Test
    void quantilesInterpolateBetweenRanks() {
        double[] v = {7, 1, 3, 5}; // sorted: 1 3 5 7
        assertThat(Statistics.median(v)).isEqualTo(4.0);
        assertThat(Statistics.quantile(v, 0.9)).isCloseTo(6.4, within(1e-12)); // h = 2.7 -> 5 + 0.7 * 2
        assertThat(Statistics.quantile(v, 0)).isEqualTo(1.0);
        assertThat(Statistics.quantile(v, 1)).isEqualTo(7.0);
        assertThat(Statistics.mean(v)).isEqualTo(4.0);
        assertThat(Statistics.max(v)).isEqualTo(7.0);
    }

    @Test
    void tiedValuesShareAverageRank() {
        assertThat(Statistics.ranks(new double[] {10, 20, 20, 5})).containsExactly(2, 3.5, 3.5, 1);
    }

    @Test
    void spearmanOfMonotonicDataIsOne() {
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {1, 4, 9, 16, 25};
        assertThat(Statistics.spearman(x, y)).isCloseTo(1.0, within(1e-12));
        assertThat(Statistics.spearman(x, new double[] {5, 4, 3, 2, 1})).isCloseTo(-1.0, within(1e-12));
    }

    @Test
    void spearmanMatchesHandComputedValueWithTies() {
        // x ranks: 1 2 3 4; y = {1, 3, 2, 2} ranks: 1 4 2.5 2.5
        // pearson(ranks): mean 2.5 each; sxy = (-1.5)(-1.5)+(-0.5)(1.5)+(0.5)(0)+(1.5)(0) = 1.5
        // sxx = 5, syy = 2.25 + 2.25 + 0 + 0 = 4.5  ->  1.5 / sqrt(22.5) = 0.316227...
        double rho = Statistics.spearman(new double[] {1, 2, 3, 4}, new double[] {1, 3, 2, 2});
        assertThat(rho).isCloseTo(1.5 / Math.sqrt(22.5), within(1e-12));
    }

    @Test
    void constantVariableHasNoCorrelation() {
        assertThat(Statistics.spearman(new double[] {1, 2, 3}, new double[] {4, 4, 4})).isNaN();
    }
}
