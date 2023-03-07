package ch.rakudave.jnetmap.net.status;

import java.net.InetAddress;

/**
 * Defines a method to determine if a device is up/down
 *
 * @author rakudave
 */
public interface PingMethod {
    /**
     * Determine if the address is reachable
     *
     * @param address to check
     * @return Status of the address
     */
    Status getStatus(InetAddress address);
}
