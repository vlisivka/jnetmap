package ch.rakudave.jnetmap.net;

import ch.rakudave.jnetmap.controller.Actions;
import ch.rakudave.jnetmap.controller.Scheduler;
import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.IF.PhysicalIF;
import ch.rakudave.jnetmap.model.IF.TransparentIF;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.Host;
import ch.rakudave.jnetmap.net.status.*;
import ch.rakudave.jnetmap.util.Tuple;
import ch.rakudave.jnetmap.util.logging.Logger;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Scans a Subnet for (alive) Hosts
 *
 * @author rakudave
 */
public class
NetworkScan implements ChangeListener {
    public static final String IP_REGEX = "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
    private Subnet subnet;
    private java.util.Map<InetAddress, PingMethod> found;
    private ChangeListener listener;
    private ExecutorService ex;

    public NetworkScan() {
    }

    public NetworkScan(Subnet subnet, ChangeListener updateListener) {
        this.subnet = subnet;
        listener = updateListener;
        found = new HashMap<>();
    }

    public void start(boolean doPortScan) {
        if (ex != null) return;
        ex = Executors.newFixedThreadPool(50);
        String[] addresses = subnet.getInfo().getAllAddresses();
        for (String address1 : addresses) {
            try {
                InetAddress address = InetAddress.getByName(address1);
                ex.submit(new HostCheck(address, this, doPortScan));
            } catch (UnknownHostException uhe) {
                Logger.error("Invalid address: " + address1, uhe);
            } catch (RejectedExecutionException ree) {
                Logger.debug("Scan interrupted: " + address1, ree);
            }
        }
        ex.shutdown();
    }

    public boolean isDone() {
        return ex.isTerminated();
    }

    public void cancel() {
        if (ex != null) ex.shutdownNow();
    }

    public java.util.Map<InetAddress, PingMethod> getFoundHosts() {
        return found;
    }

    public void addToMap(final java.util.Map<InetAddress, PingMethod> hosts, final Map map) {
        if (hosts == null || hosts.isEmpty() || map == null) return;
        // TODO devices are added at [0,0], find way to layout properly! (graphLayout.setLocation?)
        Connection gToS = new Connection();
        Device gateway;
        Device aSwitch = null;
        NetworkIF gatewayIF = null;
        try { // try to find the gateway
            gatewayIF = tryFindIP(map, InetAddress.getByName(subnet.getInfo().getLowAddress()));
        } catch (UnknownHostException ex) {
            Logger.debug("Unable to find gateway-interface", ex);
        }
        if (gatewayIF == null) { // create a gateway if none was found
            gateway = new Host("Router");
            gatewayIF = new PhysicalIF(gateway, gToS, subnet.getInfo().getLowAddress());
            map.addVertex(gateway);
            gateway.addInterface(gatewayIF);
        } else {
            gateway = gatewayIF.getDevice();
        }
        try { // try to find the switch attached to the gateway
            aSwitch = map.getOpposite(gateway, gatewayIF.getConnection());
            // if the opposite is not a switch, reset
            if (aSwitch != null && !aSwitch.getType().toLowerCase().contains("switch")) aSwitch = null;
        } catch (Exception e) {
            Logger.debug("Unable to find opposite", e);
        }
        if (aSwitch == null) { // create a switch if necessary
            aSwitch = new Host("Switch");
            map.addVertex(aSwitch);
            aSwitch.addInterface(new TransparentIF(aSwitch, gToS, gatewayIF));
            map.addEdge(gToS, gateway, aSwitch);
        }
        final Device gw = gateway, sw = aSwitch;
        final NetworkIF gif = gatewayIF;
        for (final InetAddress address : hosts.keySet()) {
            Scheduler.execute(() -> {
                try {
                    if (!address.equals(gif.getAddress())) {
                        if (tryFindIP(map, address) != null) return;
                        Logger.debug("Adding Interface " + address);
                        Connection c = new Connection();
                        Device d = new Host();
                        d.setName(address.getHostName());
                        PhysicalIF pif = new PhysicalIF(d, c, address.getHostAddress());
                        pif.setPingMethod(hosts.get(address));
                        pif.setSubnet(subnet.getInfo().getNetmask());
                        pif.setGateway(subnet.getInfo().getLowAddress());
                        d.addInterface(pif);
                        SNMP.inferProperties(d);
                        sw.addInterface(new TransparentIF(sw, c, pif));
                        map.addVertex(d);
                        map.addEdge(c, d, sw);
                    } else {
                        if (gif instanceof PhysicalIF) ((PhysicalIF) gif).setPingMethod(hosts.get(address));
                        gw.setName(address.getHostName());
                    }
                } catch (Exception e) {
                    Logger.error("Unable to add address " + address + " to map", e);
                }
            });
        }
        Actions.refresh().actionPerformed(null);
    }

    public static NetworkIF tryFindIP(Map m, InetAddress address) {
        for (Device d : m.getVertices()) {
            for (NetworkIF nif : d.getInterfaces()) {
                if (address.equals(nif.getAddress()))
                    return nif;
            }
        }
        return null;
    }

    public class HostCheck implements Runnable {
        private InetAddress address;
        private ChangeListener l;
        private boolean doPortScan;

        public HostCheck(InetAddress address, ChangeListener l,
                         boolean doPortScan) {
            this.address = address;
            this.l = l;
            this.doPortScan = doPortScan;
        }

        @Override
        public void run() {
            Tuple<InetAddress, PingMethod> result;
            // ignore A/B/C-Broadcast addresses
            if (new Subnet(address.getHostAddress() + "/24").getInfo().getBroadcastAddress().equals(address.getHostAddress()) ||
                    new Subnet(address.getHostAddress() + "/16").getInfo().getBroadcastAddress().equals(address.getHostAddress()) ||
                    new Subnet(address.getHostAddress() + "/8").getInfo().getBroadcastAddress().equals(address.getHostAddress())) {
                return;
            }
            Logger.trace("Checking if " + address.getHostAddress() + " is up");
            if (IsReachable.getInstance().getStatus(address) == Status.UP) {
                result = new Tuple<>(address,
                        IsReachable.getInstance());
            } else if (NativePing.getInstance().getStatus(address) == Status.UP) {
                result = new Tuple<>(address, NativePing
                        .getInstance());
            } else if (doPortScan) {
                int port = PortScan.sweepCommon(address);
                result = (port != -1) ? new Tuple<>(
                        address, new OpenSocket(port)) : null;
            } else {
                result = null;
            }
            l.stateChanged(new ChangeEvent(result));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void stateChanged(ChangeEvent e) {
        Tuple<InetAddress, PingMethod> tuple = (Tuple<InetAddress, PingMethod>) e.getSource();
        if (!found.containsKey(tuple.getFirst())) {
            found.put(tuple.getFirst(), tuple.getSecond());
            if (listener != null)
                listener.stateChanged(new ChangeEvent(tuple));
        }
    }

    public static String getLocalAddress() {
        Enumeration<NetworkInterface> nets;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            Logger.debug("Unable to get local interfaces", e);
            return null;
        }
        for (NetworkInterface netint : Collections.list(nets)) {
            try {
                if (netint.isLoopback() || netint.isVirtual() || !netint.isUp()) continue;
                for (InetAddress inetAddress : Collections.list(netint.getInetAddresses())) {
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().matches(IP_REGEX)) {
                        return inetAddress.getHostAddress();
                    }
                }
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
