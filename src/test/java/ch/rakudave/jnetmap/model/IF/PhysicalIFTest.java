package ch.rakudave.jnetmap.model.IF;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.Host;
import ch.rakudave.jnetmap.net.Subnet;
import ch.rakudave.jnetmap.net.status.PingMethod;
import ch.rakudave.jnetmap.net.status.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author sebehuber
 *
 */
public class PhysicalIFTest {
	public InetAddress address;
	public PingMethod method;
	public Status status;
	public Date lastSeen;
	public static PhysicalIF pif;
	public static PhysicalIF pif2;
	public String CIDR_Subnet = "255.255.255.0";
	public String address1 = "192.168.0.1";
	public String address2 = "192.168.10.1";
	public String address3 = "192.168.0.1/24";
	public String gateway = "192.168.0.1";
	Device device;
	Date newdate;
	Subnet sub = new Subnet(address3);

	@Before
	public void setUp() {
		status = Status.UP;
		lastSeen = new Date();
		Connection c = new Connection();
		c.setBandwidth(100);
		Host host = new Host();
		pif = new PhysicalIF(host, c, address1);
		pif2 = new PhysicalIF(host, c, address2);
	}

	@Test
	public void equalsTest() {
		assertTrue(pif.equals(pif));
		assertFalse(pif.equals(pif2));

	}

	@Test
	public void GetSetAddressTest() {
		assertTrue(pif.setAddress(address2));
		assertFalse(pif.setAddress("ringo"));
		assertEquals(pif.getAddress().toString().split("/")[1], address2);
	}

	@Test
	public void GetSetGatewayTest() {
		assertTrue(pif.setGateway(gateway));
		assertEquals(pif.getGateway().toString().split("/")[1], gateway);
		assertFalse(pif.setGateway("ringo"));
	}

	@Test
	public void setGetSubnetTest(){
	assertTrue(pif.setSubnet(CIDR_Subnet));
	assertFalse(pif2.setSubnet("277.344.33.2"));
	assertEquals(pif.getSubnet().getInfo().getCidrSignature(),address3);	
	}
	@Test
	public void updateStatusTest(){
	Date d= pif.getLastSeen();
	pif.updateStatus();
	assertNotSame(d,pif.getLastSeen());
		
		
	}
	
	@After
	public  void tearDown() {

		pif = null;
		pif2 = null;
	}

}
