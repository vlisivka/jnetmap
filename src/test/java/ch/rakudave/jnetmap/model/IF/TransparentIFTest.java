package ch.rakudave.jnetmap.model.IF;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.device.Host;
import ch.rakudave.jnetmap.net.Subnet;
import ch.rakudave.jnetmap.net.status.PingMethod;
import ch.rakudave.jnetmap.net.status.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;

import static org.junit.Assert.*;

/**
 * @author sebehuber
 * 
 */
public class TransparentIFTest {
	public InetAddress address;
	public PingMethod method;
	public Status status;
	public static PhysicalIF pif;
	public static PhysicalIF pif2;
	public TransparentIF tif;
	public TransparentIF tif2;
	public String CIDR_Subnet = "255.255.255.0";
	public String address1 = "192.168.0.1";
	public String address2 = "192.168.10.1";
	public String address3 = "192.168.0.1/24";
	public String gateway = "192.168.0.1";
	Subnet sub = new Subnet(address3);

	@Before
	public void setUp() {
		Connection c = new Connection();
		c.setBandwidth(100);
		Host host = new Host();
		pif = new PhysicalIF(host, c, address1);
		pif2 = new PhysicalIF(host, c, address2);
		tif = new TransparentIF(host, c, pif);
		tif2 = new TransparentIF(host, c, pif2);
	}

	@Test
	public void equalsTest() {
		assertTrue(tif.equals(tif));
		assertFalse(tif.equals(tif2));
	}

	@Test
	public void setCounterpartTest() {
		tif.setCounterpart(pif2);
		assertSame(tif.getCounterpart(), pif2);
	}

	@After
	public void tearDown() {
		tif = null;
		tif2 = null;
		pif = null;
		pif2 = null;
	}

}
