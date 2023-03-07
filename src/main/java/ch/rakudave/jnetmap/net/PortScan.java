package ch.rakudave.jnetmap.net;

import ch.rakudave.jnetmap.util.IO;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PortScan {
    // list of ports that are most likely to be open, in part according to http://www.speedguide.net/ports_common.php
    private static final int[] commonlyOpen = new int[]{22, 135, 139, 445, 80, Settings.getInt("snmp.port", 161), 21, 53};
    private static Properties ports;

    public static Map<Integer, String> wellKnownPortsScan(InetAddress address) {
        return scan(address, 0, 1023);
    }

    public static Map<Integer, String> registeredPortscan(InetAddress address) {
        return scan(address, 1024, 49151);
    }

    public static Map<Integer, String> allPortsScan(InetAddress address) {
        return scan(address, 0, 65535);
    }

    public static Map<Integer, String> scan(InetAddress address, int fromPort, int toPort) {
        if (fromPort < 0 || fromPort > 635535 || toPort < 0 || toPort > 635535 || fromPort > toPort) return null;
        if (ports == null) load();
        final Map<Integer, String> map = new HashMap<>();
        int split = (toPort - fromPort) % 4;
        if (split < 3) {
            Thread t1 = spawn(map, address, fromPort, toPort + 1);
            t1.start();
            try {
                t1.join();
            } catch (InterruptedException e) {
                Logger.error("Something interrupted the port-scanning thread", e);
            }
        } else {
            Thread t1 = spawn(map, address, fromPort, fromPort + split);
            Thread t2 = spawn(map, address, fromPort + split, fromPort + (2 * split));
            Thread t3 = spawn(map, address, fromPort + (2 * split), fromPort + (3 * split));
            Thread t4 = spawn(map, address, fromPort + (3 * split), toPort + 1);
            t1.start();
            t2.start();
            t3.start();
            t4.start();
            try {
                t1.join();
                t2.join();
                t3.join();
                t4.join();
            } catch (InterruptedException e) {
                Logger.error("Something interrupted a port-scanning thread", e);
            }
        }
        return map;
    }

    public static boolean isOpen(InetAddress address, int port) {
        try {
            SocketAddress sockaddr = new InetSocketAddress(address, port);
            Socket sock = new Socket();
            sock.connect(sockaddr, Settings.getInt("portscan.timeout", 250));
            sock.close();
            return true;
        } catch (Exception e) {
            return false; // -> port closed
        }
    }

    public static int sweepCommon(InetAddress address) {
        for (int port : commonlyOpen) {
            if (isOpen(address, port)) return port;
        }
        return -1;
    }

    private PortScan() {
    }

    private static synchronized void load() {
        ports = IO.getMergedProps("/ports");
    }

    private static Thread spawn(final Map<Integer, String> map, final InetAddress address, final int fromPort, final int toPort) {
        return new Thread(() -> {
            for (int i = fromPort; i < toPort; i++) {
                if (isOpen(address, i)) {
                    map.put(i, ports.getProperty(String.valueOf(i), "---"));
                }
            }
        });
    }
}
