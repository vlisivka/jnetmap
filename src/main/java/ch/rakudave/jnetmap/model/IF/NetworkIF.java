package ch.rakudave.jnetmap.model.IF;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.net.Subnet;
import ch.rakudave.jnetmap.net.status.PingMethod;
import ch.rakudave.jnetmap.net.status.Status;
import ch.rakudave.jnetmap.util.Tuple;

import java.net.InetAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Defines a network interface, consisting of (at least) an address, a type and a method to determine its state.
 *
 * @author rakudave
 */
public interface NetworkIF {

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    boolean equals(Object o);

    /**
     * @return the address of this network interface
     */
    InetAddress getAddress();

    /**
     * @return the canonical name reported by the address of this network interface
     */
    String getCanonicalName();

    /**
     * @return the connection this interface is associated with
     */
    Connection getConnection();

    /**
     * @return the device that this network interface is attached to
     * @see Device
     */
    Device getDevice();

    /**
     * @return the default gateway of this interface.
     */
    InetAddress getGateway();

    /**
     * @return when this link has been up the last time
     * @see Date
     */
    Date getLastSeen();

    /**
     * @return how long it took the IF to respond to the last ping, in milliseconds
     */
    long getLatency();

    /**
     * @return the name of this network interface, e.g. eth0
     */
    String getName();

    /**
     * @return the status of this network interface
     * @see Status
     */
    Status getStatus();

    /**
     * @return the subnet mask of this network interface.
     */
    Subnet getSubnet();

    /**
     * Sets a new network address for this network interface
     *
     * @param addr The network address to be set
     * @return success, i.e. false if the address was invalid
     */
    boolean setAddress(String addr);

    /**
     * Set a new default gateway for this network interface
     *
     * @param gateway the address of the new gateway
     */
    boolean setGateway(String gateway);

    /**
     * Get a history of status changes, used for graphs etc.
     *
     * @return A list of date/status-tuples
     */
    LinkedList<Tuple<Date, Status>> getStatusHistory();

    /**
     * Put status "unknown" at the end of the status history, used when exiting the program
     */
    void addStatusUnknownToHistory();

    /**
     * set the name reported by the address of this network interface, e.g. eth0
     *
     * @param name
     */
    void setName(String name);

    /**
     * Set a new subnet mask for this network interface
     *
     * @param subnet the address of the subnet
     */
    boolean setSubnet(String subnet);

    /**
     * Request this interface to check its status using a specified method
     *
     * @see Status
     * @see PingMethod
     */
    void updateStatus();

    String toHtmlString();
}