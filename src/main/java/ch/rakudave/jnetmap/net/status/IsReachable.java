package ch.rakudave.jnetmap.net.status;

import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.net.InetAddress;

/**
 * @author rakudave
 */

@XStreamAlias("IsReachable")
public class IsReachable implements PingMethod {
    private static IsReachable instance;
    private static final Object lock = new Object();

    public static IsReachable getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new IsReachable();
                }
            }
        }
        return instance;
    }

    protected IsReachable() {
    }

    @Override
    public Status getStatus(InetAddress address) {
        try {
            if (address.isReachable(Settings.getInt("isreachable.timeout", Settings.getInt("ping.timeout", 5000)))) {
                Logger.trace(address + " is up");
                return Status.UP;
            } else {
                Logger.trace(address + " is down");
                return Status.DOWN;
            }
        } catch (Exception e) {
            Logger.trace("Failed to reach " + address, e);
            return Status.UNKNOWN;
        }
    }

    @Override
    public String toString() {
        return "Java Ping";
    }

}
