package fx.cbo;

/*
 * Coupling through a call chain: Notifier never names Channel, but it calls
 * Channel.open() on the value returned by Hub.channel().
 *
 *   type      uses                       used by              CBO
 *   Notifier  Hub, Channel (via call)    -                    2
 *   Hub       Channel                    Notifier             2
 *   Channel   -                          Hub, Notifier        2
 */
public class Notifier {

    void send() {
        Hub.get().channel().open();
    }
}

class Hub {
    private static final Hub INSTANCE = null;

    static Hub get() {
        return INSTANCE;
    }

    Channel channel() {
        return null;
    }
}

class Channel {
    void open() {
    }
}
