package fx.lcom;

/*
 * Single: one method -> LCOM 0, LCOM* undefined.
 * Stateless: no fields -> two methods share nothing: P = 1, Q = 0, LCOM 1; LCOM* undefined.
 */
class Single {
    private int value;

    int get() {
        return value;
    }
}

class Stateless {
    int one() {
        return 1;
    }

    int two() {
        return 2;
    }
}
