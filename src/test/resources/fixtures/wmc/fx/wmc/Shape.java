package fx.wmc;

/*
 * area() has no body and weighs 1; isLarge() is 1 + if = 2.  WMC = 3.
 */
public interface Shape {

    double area();

    default boolean isLarge() {
        if (area() > 10.0) {
            return true;
        }
        return false;
    }
}
