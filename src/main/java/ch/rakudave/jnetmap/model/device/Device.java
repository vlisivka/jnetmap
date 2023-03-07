package ch.rakudave.jnetmap.model.device;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.net.status.Status;
import ch.rakudave.jnetmap.util.Tuple;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Defines any connectable device on a network. The actual connections are handles by
 * one or more NetworkIFs, so this only serves as "container" for all those NetworkIFs and is
 * responsible for saving its current location on the screen as well as device-specific infos
 * such as vendor and model.
 *
 * @author rakudave
 */
@XStreamAlias("Device")
public interface Device extends Comparable<Device> {
    /**
     * The type of this device, e.g. Router, Switch etc...
     * This is mostly used to paint the map
     */
    String[] defaultTypes = {
            "Cellphone", "Cloud", "Database", "Firewall", "Hub", "IP_Phone", "L3Switch", "Laptop", "Managed_Switch",
            "Modem", "Multimedia", "NAS", "Other", "PBX", "PDA", "PLC", "Printer", "Router", "Router_Firewall",
            "Server", "Switch", "Video", "Wireless", "Workstation"
    };
    String otherType = "Other", fallbackType = "Workstation";

    /**
     * Add an network interface to this device.
     *
     * @param i The interface to be added
     * @see NetworkIF
     */
    void addInterface(NetworkIF i);

    /**
     * Add a listener to this device. This listener will be notified every time
     * this device changes its state, properties have changed or a device is added/removed
     *
     * @param listener The device listener to be added
     * @see DeviceListener
     * @see DeviceEvent
     */
    void addDeviceListener(DeviceListener listener);

    @Override
    boolean equals(Object o);

    /**
     * @return device description, e.g. "Ubuntu Linux 2.6 x64"
     */
    String getDesctription();

    /**
     * Get all interfaces that are attached to this device
     *
     * @return a list of interfaces
     * @see NetworkIF
     */
    List<NetworkIF> getInterfaces();

    /**
     * Query a device for the NetworkIF which is attached to a Connection
     *
     * @param c the connection
     * @return matching NetworkIF or null if not found
     */
    NetworkIF getInterfaceFor(Connection c);

    /**
     * @return the last time any interface of this device has been up.
     */
    Date getLastSeen();

    /**
     * @return Location of this device, such as "742 Evergreen Terrace, 2nd floor"
     */
    String getLocation();

    /**
     * @return the model number of this device
     */
    String getModel();

    /**
     * @return the name of this device
     */
    String getName();

    /**
     * @return If Type == Other, this will specify the custom type identifier
     */
    String getOtherID();

    /**
     * @return the best status of all interfaces
     * (i.e. "up" if at least one interface is up)
     * @see Status
     */
    Status getStatus();

    /**
     * @return The type of the device
     */
    String getType();

    /**
     * @return the vendor of this device
     */
    String getVendor();

    @Override
    int hashCode();

    /**
     * Notifies all listeners of this device
     *
     * @param e a DeviceEvent to be sent along
     * @see DeviceEvent
     */
    void notifyListeners(DeviceEvent e);

    /**
     * Removes an interface from this device
     *
     * @param i Interface to be removed
     * @see NetworkIF
     */
    void removeInterface(NetworkIF i);

    /**
     * Removes a listener from this device
     *
     * @param listener to be removed
     * @see DeviceListener
     */
    void removeDeviceListener(DeviceListener listener);

    /**
     * Set a description of this device, e.g. "Ubuntu Linux 2.6 x64"
     *
     * @param description
     */
    void setDescription(String description);

    /**
     * Location of this device, such as "742 Evergreen Terrace, 2nd floor"
     *
     * @param location
     */
    void setLocation(String location);

    /**
     * Set the model number of this device
     *
     * @param model number
     */
    void setModel(String model);

    /**
     * Set a new name for this device
     *
     * @param name
     */
    void setName(String name);

    /**
     * Set the ID of what exactly is meant when setting Type = Other
     *
     * @param other identification string
     */
    void setOtherID(String other);

    /**
     * Set device type
     *
     * @param type of this device
     */
    void setType(String type);

    /**
     * Set the vendor of this device
     *
     * @param vendor
     */
    void setVendor(String vendor);

    /**
     * Updates the state of this device by updating all attached interfaces
     */
    void updateStatus();

    /**
     * Add metadata to a device, such as notes etc.
     *
     * @param id   Identifier
     * @param data Data
     */
    void setMetadata(String id, String data);

    /**
     * Retrieve previously stored metadata
     *
     * @param id Identifier
     * @return data or null if not found
     */
    String getMetadata(String id);

    /**
     * Removes previously stored metadata
     *
     * @param id Identifier
     */
    void removeMetadata(String id);

    /**
     * Get a history of status changes, used for graphs etc.
     *
     * @return A list of date/status-tuples
     */
    LinkedList<Tuple<Date, Status>> getStatusHistory();

    /**
     * Put status "unknown" at the end of the status history, used when exiting the program
     * Cascades to all attached network interfaces!
     */
    void addStatusUnknownToHistory();

    String toHtmlString();

    /**
     * @return if this device should be ignored (generate no status events)
     */
    boolean isIgnore();

    /**
     * Whether to ignore this device (generate no status events)
     *
     * @param ignore
     */
    void setIgnore(boolean ignore);

    /**
     * The number of network ports this device has
     */
    int getNrOfPorts();

    /**
     * Set the number of network ports this device has
     *
     * @param nrOfPorts
     */
    void setNrOfPorts(int nrOfPorts);
}