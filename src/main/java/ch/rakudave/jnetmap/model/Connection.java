package ch.rakudave.jnetmap.model;

import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.net.status.Status;
import ch.rakudave.jnetmap.util.logging.Logger;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rakudave
 */
public class Connection {
    /**
     * The type of cable used to connect this interface and its counterpart. This is mostly used to paint the map.
     */
    public enum Type {
        Coaxial, Ethernet, Fiber, Phone, Serial, Tunnel, Wireless
    }
    public static final double DEFAULT_BANDWIDTH = 1000;

    private Type type;
    private Status status;
    private double bandwidth;
    @XStreamOmitField
    private Long latency;
    private String name;
    private Map<NetworkIF, Status> statusMap;

    /**
     * Create a new Connection with the type Ethernet and a bandwidth of 100Mb/s
     *
     * @see Type
     */
    public Connection() {
        this(Type.Ethernet, DEFAULT_BANDWIDTH);
    }

    /**
     * Create a new Connection of a certain type and a bandwidth of 100Mb/s
     *
     * @param type of cable
     * @see Type
     */
    public Connection(Type type) {
        this(type, 1000);
    }

    /**
     * Create a new Connection of a certain type and bandwidth
     *
     * @param type      of cable
     * @param bandwidth in megabits per second
     */
    public Connection(Type type, double bandwidth) {
        setType(type);
        setBandwidth(bandwidth);
        status = Status.UNKNOWN;
        statusMap = new HashMap<>(2);
    }

    /**
     * @return the bandwidth of this link in megabits per second
     */
    public double getBandwidth() {
        return bandwidth;
    }

    /**
     * @return The current status of this connection, i.e. the combined status of the hosts that are connected with this connection
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return The type of cable used to connect two devices
     */
    public Type getType() {
        return type;
    }

    /**
     * Set the bandwidth of this connection
     *
     * @param bandwidth in megabits per second
     */
    public void setBandwidth(double bandwidth) {
        if (bandwidth > 0) this.bandwidth = bandwidth;
    }

    /**
     * Set the status of this connection
     *
     * @param netStatus the combined status of the hosts that are connected with this connection
     */
    public void setStatus(NetworkIF netIF, Status netStatus) {
        if (netIF == null) return;
        statusMap.put(netIF, netStatus);
        Logger.trace("Updating connection status from " + netIF.getName() + ": " + netStatus);
        Status newStatus = null;
        for (Status s : statusMap.values()) {
            if (newStatus == null || newStatus.compareTo(s) > 0) newStatus = s;
        }
        if (status != newStatus) {
            status = newStatus;
            Logger.trace("Connection status was " + status + ", is " + newStatus);
        }
    }

    public void updateStatus() {
        for (NetworkIF nif : statusMap.keySet()) {
            nif.updateStatus();
        }
    }

    /**
     * Set the type of cable used to connect two devices
     *
     * @param type of cable
     * @see Type
     */
    public void setType(Type type) {
        this.type = type;
    }

    public Long getLatency() {
        return latency;
    }

    public void setLatency(Long latency) {
        this.latency = latency;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void clearStatusMap() {
        statusMap.clear();
    }
}
