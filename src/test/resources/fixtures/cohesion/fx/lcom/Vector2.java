package fx.lcom;

/*
 * length {x, y}   scale {x, y}   setX {x}  -- in setX, the local variable y shadows
 * the field y, so only x counts.
 *
 * Every pair shares x: P = 0, Q = 3, LCOM = 0.
 * mu(x) = 3, mu(y) = 2, m = 3, a = 2:  LCOM* = (2.5 - 3) / (1 - 3) = 0.25
 */
public class Vector2 {

    private double x;
    private double y;

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public void scale(double k) {
        x *= k;
        y *= k;
    }

    public void setX(double value) {
        double y = value;
        this.x = y;
    }
}
