package ch.rakudave.jnetmap.model.IF;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.net.Subnet;
import ch.rakudave.jnetmap.net.status.Status;
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

@XStreamAlias("LogicalIF")
public class LogicalIF implements NetworkIF {
    private String name = "";
    private InetAddress address;
    private Subnet subnet;
    private Device device;
    private Connection connection;

    public LogicalIF() {
    }

    public LogicalIF(Device parent, Connection connection, String addr) {
        device = parent;
        this.connection = connection;
        setAddress(addr);
        name = "lo" + parent.getInterfaces().size();
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
        return null;
    }

    @Override
    public Date getLastSeen() {
        return device.getLastSeen();
    }

    @Override
    public String getName() {
        return address.getHostName();
    }

    @Override
    public Status getStatus() {
        return device.getStatus();
    }

    @Override
    public Subnet getSubnet() {
        if (subnet == null) subnet = new Subnet("127.0.0.1/24");
        return subnet;
    }

    @Override
    public boolean setAddress(String addr) {
        try {
            address = InetAddress.getByName(addr);
            return true;
        } catch (UnknownHostException e) {
            System.err.println("Address not found: " + addr);
            return false;
        }
    }

    @Override
    public boolean setGateway(String gateway) {
        return true;
    }

    @Override
    public void setName(String name) {
        if (name != null) this.name = name;
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

        connection.setStatus(this, getStatus());
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public LinkedList<Tuple<Date, Status>> getStatusHistory() {
        return device.getStatusHistory();
    }

    @Override
    public void addStatusUnknownToHistory() {
        // do nothing, will be redone by device
    }

    @Override
    public long getLatency() {
        return 0;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
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

}
