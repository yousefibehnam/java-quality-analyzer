package fx.lcom;

/*
 * Methods considered: increment, reset, getLabel, help (constructor excluded).
 * Fields: count, step, label (a = 3, m = 4).
 *
 *   increment {count, step}   reset {count}   getLabel {label}   help {}
 *
 * Pairs (6): only increment-reset share a field.  Q = 1, P = 5,  LCOM = 5 - 1 = 4.
 * mu(count) = 2, mu(step) = 1, mu(label) = 1 (the constructor does not count).
 * LCOM* = (4/3 - 4) / (1 - 4) = 8/9 = 0.8889
 */
public class Counter {

    private int count;
    private int step = 1;
    private final String label;

    public Counter(String label) {
        this.label = label;
    }

    public void increment() {
        count += step;
    }

    public void reset() {
        this.count = 0;
    }

    public String getLabel() {
        return label;
    }

    public static String help() {
        return "counts things";
    }
}
