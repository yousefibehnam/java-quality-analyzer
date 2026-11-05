package io.github.yousefibehnam.jqa.summary;

import java.util.Arrays;

/** Descriptive statistics and rank correlation used by the summary report. */
public final class Statistics {

    private Statistics() {
    }

    public static double mean(double[] values) {
        requireNonEmpty(values);
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    /**
     * The p-th quantile (0 &lt;= p &lt;= 1) by linear interpolation between closest
     * ranks (the "type 7" definition used by default in R and NumPy).
     */
    public static double quantile(double[] values, double p) {
        requireNonEmpty(values);
        if (p < 0 || p > 1) {
            throw new IllegalArgumentException("p must be in [0, 1]: " + p);
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        double h = (sorted.length - 1) * p;
        int lo = (int) Math.floor(h);
        int hi = Math.min(lo + 1, sorted.length - 1);
        return sorted[lo] + (h - lo) * (sorted[hi] - sorted[lo]);
    }

    public static double median(double[] values) {
        return quantile(values, 0.5);
    }

    public static double max(double[] values) {
        requireNonEmpty(values);
        return Arrays.stream(values).max().getAsDouble();
    }

    /** Ranks starting at 1; tied values share the average of their ranks. */
    static double[] ranks(double[] values) {
        int n = values.length;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(values[a], values[b]));
        double[] ranks = new double[n];
        int i = 0;
        while (i < n) {
            int j = i;
            while (j + 1 < n && values[order[j + 1]] == values[order[i]]) {
                j++;
            }
            double average = (i + j) / 2.0 + 1;
            for (int k = i; k <= j; k++) {
                ranks[order[k]] = average;
            }
            i = j + 1;
        }
        return ranks;
    }

    /** Spearman's rank correlation; NaN when either variable is constant. */
    public static double spearman(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("different lengths: " + x.length + " and " + y.length);
        }
        if (x.length < 2) {
            return Double.NaN;
        }
        return pearson(ranks(x), ranks(y));
    }

    static double pearson(double[] x, double[] y) {
        double mx = mean(x);
        double my = mean(y);
        double sxy = 0;
        double sxx = 0;
        double syy = 0;
        for (int i = 0; i < x.length; i++) {
            sxy += (x[i] - mx) * (y[i] - my);
            sxx += (x[i] - mx) * (x[i] - mx);
            syy += (y[i] - my) * (y[i] - my);
        }
        if (sxx == 0 || syy == 0) {
            return Double.NaN;
        }
        return sxy / Math.sqrt(sxx * syy);
    }

    private static void requireNonEmpty(double[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("no values");
        }
    }
}
