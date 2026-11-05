package fx.model;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;

/*
 * Structural cases for the model builder:
 *  - member types (Inner, Nested) are separate classes; anonymous classes are not;
 *  - the anonymous class body belongs to supplier(), so supplier() uses field hits;
 *  - Inner.read() uses Outer's field, which is not one of Inner's own fields;
 *  - a statically imported method is attributed to the importing type's owner.
 */
public class Outer {

    private int hits;

    public class Inner {
        int read() {
            return hits;
        }
    }

    static class Nested {
    }

    public Supplier<Integer> supplier() {
        return new Supplier<Integer>() {
            @Override
            public Integer get() {
                hits++;
                return hits;
            }
        };
    }

    public String name(String s) {
        return requireNonNull(s);
    }
}

record Pair(String left, String right) {

    Pair {
        requireNonNull(left);
    }

    int size() {
        return left.length() + right.length();
    }
}
