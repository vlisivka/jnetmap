package ch.rakudave.jnetmap.model.IF;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.device.Host;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.UnknownHostException;

import static org.junit.Assert.*;

/**
 * @author sebehuber
 *
 */

public class LogicalIFTest {
	private LogicalIF LIF;
	private LogicalIF LIF2;
	private String address;
	private String address2;
	private String failaddress;

	@Before
	public void setUP() {
		Host parent = new Host();
		address = "192.168.0.16";
		address2 = "170.234.120.11";
		failaddress = "Ringo";
		LIF = new LogicalIF(parent, new Connection(), address);
		LIF2 = new LogicalIF(parent, new Connection(), address);
	}

	@Test
	public void setAddressTest() {
		assertTrue(LIF2.setAddress(address2));
	}

	@Test
	
	public void setAddressTest2() throws UnknownHostException {
		assertFalse(LIF2.setAddress(failaddress));
	}
	@Test
	public void setGetSubnetTest(){
	assertFalse(LIF.setSubnet("277.344.33.2"));
	assertTrue(LIF.setSubnet("255.255.255.252"));
	assertEquals("255.255.255.252",LIF.getSubnet().getInfo().getNetmask());
	}
	@After
	public void tearDown() {
		LIF = null;
		LIF2 = null;

	}

}