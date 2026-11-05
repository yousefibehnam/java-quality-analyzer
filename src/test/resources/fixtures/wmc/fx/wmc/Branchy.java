package fx.wmc;

/*
 * Hand-counted cyclomatic complexity (1 + decision points):
 *
 *   straight      1                                  = 1
 *   ifElse        1 + if                             = 2   (else adds nothing)
 *   loops         1 + for + for-each + while + do    = 5
 *   classicSwitch 1 + case 1 + case 2 + case 3       = 4   (default adds nothing)
 *   arrowSwitch   1 + labels 1, 2 + label 3          = 4
 *   logic         1 + && + ||                        = 3
 *   ternaryCatch  1 + ?: + catch                     = 3
 *   constructor   1 + if                             = 2
 *                                              WMC   = 24
 */
public class Branchy {

    private final int limit;

    public Branchy(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("negative");
        }
        this.limit = limit;
    }

    public int straight() {
        return limit;
    }

    public int ifElse(int x) {
        if (x > 0) {
            return 1;
        } else {
            return 2;
        }
    }

    public int loops(int[] xs) {
        int s = 0;
        for (int i = 0; i < xs.length; i++) {
            s += xs[i];
        }
        for (int x : xs) {
            s += x;
        }
        while (s > 100) {
            s--;
        }
        do {
            s++;
        } while (s < 0);
        return s;
    }

    public String classicSwitch(int d) {
        switch (d) {
            case 1:
                return "one";
            case 2:
            case 3:
                return "few";
            default:
                return "many";
        }
    }

    public String arrowSwitch(int d) {
        return switch (d) {
            case 1, 2 -> "low";
            case 3 -> "mid";
            default -> "high";
        };
    }

    public boolean logic(boolean a, boolean b, boolean c) {
        return a && b || c;
    }

    public int ternaryCatch(String s) {
        try {
            return s.isEmpty() ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
