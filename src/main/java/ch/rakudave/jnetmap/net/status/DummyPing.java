package ch.rakudave.jnetmap.net.status;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.net.InetAddress;

/**
 * @author rakudave
 */

@XStreamAlias("DummyPing")
public class DummyPing implements PingMethod {

    @Override
    public Status getStatus(InetAddress address) {
        return Status.UNKNOWN;
    }

    @Override
    public String toString() {
        return "Dummy Ping";
    }
}
