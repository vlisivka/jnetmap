package ch.rakudave.jnetmap.model.IF;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.DeviceEvent;
import ch.rakudave.jnetmap.model.device.DeviceEvent.Type;
import ch.rakudave.jnetmap.net.Subnet;
import ch.rakudave.jnetmap.net.status.IsReachable;
import ch.rakudave.jnetmap.net.status.PingMethod;
import ch.rakudave.jnetmap.net.status.Status;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.Tuple;
import ch.rakudave.jnetmap.util.logging.Logger;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rakudave
 */
@XStreamAlias("PhysicalIF")
public class PhysicalIF implements NetworkIF {
    private InetAddress address, gateway;
    private Subnet subnet;
    private String name = "", macAddress;
    private Device device;
    private Connection connection;
    private PingMethod method;
    private Status status;
    private Date lastSeen;
    private LinkedList<Tuple<Date, Status>> statusHistory;
    private long latency;
    private boolean ignore;

    public PhysicalIF() {
    }

    public PhysicalIF(Device parent, Connection connection, String addr) {
        this(parent, connection, addr, "", IsReachable.getInstance());
    }

    public PhysicalIF(Device parent, Connection connection, String addr, PingMethod m) {
        this(parent, connection, addr, "", m);
    }

    public PhysicalIF(Device parent, Connection connection, String macAddress, String addr) {
        this(parent, connection, addr, macAddress, IsReachable.getInstance());
    }

    public PhysicalIF(Device parent, Connection connection, String addr, String macAddress, PingMethod m) {
        device = parent;
        this.connection = connection;
        setAddress(addr);
        setMacAddress(macAddress);
        setPingMethod(m);
        status = Status.UNKNOWN;
        name = "eth" + (parent.getInterfaces() != null ? parent.getInterfaces().size() : 0);
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public String getCanonicalName() {
        return address.getCanonicalHostName();
    }

    @Override
    public Device getDevice() {
        return device;
    }

    @Override
    public InetAddress getGateway() {
        return gateway;
    }

    @Override
    public Date getLastSeen() {
        return lastSeen;
    }

    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public Subnet getSubnet() {
        return subnet;
    }

    @Override
    public boolean setAddress(String addr) {
        try {
            address = InetAddress.getByName(addr);
            return true;
        } catch (UnknownHostException e) {
            status = Status.NOT_FOUND;
            Logger.warn("Address not found: " + addr);
            return false;
        }
    }

    @Override
    public boolean setGateway(String gateway) {
        try {
            this.gateway = InetAddress.getByName(gateway);
            return true;
        } catch (UnknownHostException e) {
            Logger.warn("Gateway not found: " + gateway);
            return false;
        }
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    /**
     * Set a new method that this network interface should use to determine its status
     *
     * @param m The new method to be used
     * @see PingMethod
     */
    public void setPingMethod(PingMethod m) {
        if (m != null) method = m;
    }

    @Override
    public boolean setSubnet(String subnet) {
        if (subnet == null || subnet.isEmpty()) return false;
        if (!(subnet.contains("."))) {
            try {
                this.subnet = new Subnet(subnet);
            } catch (Exception e) {
                Logger.warn("Invalid CIDR-format for subnet " + subnet);
                return false;
            }
        } else {
            try {
                this.subnet = new Subnet(address.getHostAddress(), subnet);
            } catch (Exception e) {
                Logger.warn("Address undefined or Subnet not found: " + subnet);
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return name + ": " + address.getHostAddress();
    }

    @Override
    public String toHtmlString() {
        return "<span style=\"color: " + getStatus().getHtmlValue() + "\">" + toString() + "</span>";
    }

    @Override
    public void updateStatus() {
        long millis = System.currentTimeMillis();
        Status newStatus = method.getStatus(address);
        latency = System.currentTimeMillis() - millis;
        connection.setStatus(this, newStatus);
        if (address != null) connection.setLatency(latency);
        if (status != newStatus) {
            status = newStatus;
            Date now = new Date(millis);
            lastSeen = now;
            putInStatusHistory(now, newStatus);
            if (!ignore) device.notifyListeners(new DeviceEvent(device, Type.INTERFACE_STATUS_CHANGED, this));
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void setName(String name) {
        if (name != null) this.name = name;
    }

    /**
     * Get the method that this network interface uses to determine its status
     *
     * @returnThe new method to be used
     * @see PingMethod
     */
    public PingMethod getPingMethod() {
        return method;
    }

    @Override
    public LinkedList<Tuple<Date, Status>> getStatusHistory() {
        if (statusHistory == null) statusHistory = new LinkedList<>();
        return statusHistory;
    }

    private void putInStatusHistory(Date date, Status status) {
        if (statusHistory == null) statusHistory = new LinkedList<>();
        if (status != null && (statusHistory.size() == 0 || !status.equals(statusHistory.getLast().getSecond()))) {
            statusHistory.add(new Tuple<>(date, status));
            if (statusHistory.size() > Settings.getInt("device.history.maxsize", 20)) statusHistory.removeFirst();
        }
    }

    @Override
    public void addStatusUnknownToHistory() {
        putInStatusHistory(new Date(System.currentTimeMillis()), Status.UNKNOWN);
    }

    @Override
    public long getLatency() {
        return latency;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public void setGateway(InetAddress gateway) {
        this.gateway = gateway;
    }

    public void setSubnet(Subnet subnet) {
        this.subnet = subnet;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * @return if this device should be ignored (generate no status events)
     */
    public boolean isIgnore() {
        return ignore;
    }

    /**
     * Whether to ignore this device (generate no status events)
     *
     * @param ignore
     */
    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }
}
