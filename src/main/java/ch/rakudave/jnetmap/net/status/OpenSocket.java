package ch.rakudave.jnetmap.net.status;

import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.IOException;
import java.net.*;

/**
 * @author rakudave
 */

@XStreamAlias("OpenSocket")
public class OpenSocket implements PingMethod {
    private int port;

    public OpenSocket() throws IllegalArgumentException {
        this(0);
    }

    public OpenSocket(int port) throws IllegalArgumentException {
        if (port < 0 | port > 65535) throw new IllegalArgumentException();
        this.port = port;
    }

    @Override
    public Status getStatus(InetAddress address) {
        int timeout = Settings.getInt("socket.timeout", Settings.getInt("ping.timeout", 5000));
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(address, port), timeout);
            socket.close();
            return Status.UP;
        } catch (SocketTimeoutException e) {
            Logger.trace(address + " timed out after " + timeout, e);
            return Status.DOWN;
        } catch (SocketException e) {
            Logger.trace("Can't connect to " + address, e);
            return Status.DOWN;
        } catch (Exception e) {
            Logger.trace("Failed to get port status for " + address, e);
            return Status.UNKNOWN;
        } finally {
            try {
                socket.close();
            } catch (IOException e) { /*stfu already*/ }
        }
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "TCP Port";
    }
}
