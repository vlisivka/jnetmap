package ch.rakudave.jnetmap.net.status;

import ch.rakudave.jnetmap.util.IO;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.net.InetAddress;

/**
 * @author rakudave
 */

@XStreamAlias("NativePing")
public class NativePing implements PingMethod {
    private static NativePing instance;
    private static final Object lock = new Object();

    public static NativePing getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new NativePing();
                }
            }
        }
        return instance;
    }

    protected NativePing() {
    }

    @Override
    public Status getStatus(InetAddress address) {
        Process process = null;
        try {
            // this uses the exit-status of ping, where 0 is success
            String command = (IO.isUnix) ? Settings.get("ping.syntax.unix", "ping -c 4 -w 5") : Settings.get("ping.syntax.windows", "ping -w 1250");
            process = Runtime.getRuntime().exec(command + " " + address.getHostAddress());
            return (process.waitFor() == 0) ? Status.UP : Status.DOWN;
        } catch (Exception e) {
            Logger.trace("Failed to get native ping status " + address, e);
            return Status.UNKNOWN;
        } finally {
            if (process != null) process.destroy();
        }
    }

    @Override
    public String toString() {
        return "System Ping";
    }
}