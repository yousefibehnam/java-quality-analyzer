package fx.cbo;

import java.util.ArrayList;
import java.util.List;

/*
 * Hand-derived CBO (JDK types excluded by default; "uses" = efferent, "used by" = afferent):
 *
 *   type      uses                       used by              CBO
 *   Order     Customer, Line, Money      Report               4
 *   Customer  -                          Order                1
 *   Line      Product                    Order                2
 *   Product   -                          Line                 1
 *   Money     -                          Order                1
 *   Report    Order                      -                    1
 *
 * With JDK types included, Order also uses java.util.List and java.util.ArrayList: CBO 6.
 */
public class Order {

    private Customer customer;
    private final List<Line> lines = new ArrayList<>();

    public Customer customer() {
        return customer;
    }

    public Money total() {
        return Money.zero();
    }
}
