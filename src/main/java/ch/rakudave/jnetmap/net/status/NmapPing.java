package ch.rakudave.jnetmap.net.status;

import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.net.InetAddress;
import java.util.Scanner;

/**
 * @author rakudave
 */

@XStreamAlias("NmapPing")
public class NmapPing implements PingMethod {
    private static NmapPing instance;
    private static final Object lock = new Object();

    public static NmapPing getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new NmapPing();
                }
            }
        }
        return instance;
    }

    private NmapPing() {
    }

    @Override
    public Status getStatus(InetAddress address) {
        Process process = null;
        try {
            String command = Settings.get("ping.nmap.syntax", "nmap -sP");
            Logger.debug(command + " " + address.getHostAddress());
            process = Runtime.getRuntime().exec(command + " " + address.getHostAddress());
            // since nmap does not return a non-zero exit status when a host is down, parse stout instead :-/
            try (Scanner s = new Scanner(process.getInputStream())) {
                if (s.useDelimiter("\\A").hasNext() &&
                        s.next().contains(Settings.get("ping.nmap.contains", "Host is up"))) return Status.UP;
            }
            process.waitFor();
            return Status.DOWN;
        } catch (Exception e) {
            Logger.trace("Failed to get nmap ping status " + address, e);
            return Status.UNKNOWN;
        } finally {
            if (process != null) process.destroy();
        }
    }

    @Override
    public String toString() {
        return "Nmap Ping";
    }
}