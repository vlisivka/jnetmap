
/**
 * 
 * @author sebehuber
 *
 */
package ch.rakudave.jnetmap.net.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;

public class OpenSocketTest {
	OpenSocket socket;
	OpenSocket socket2;
	OpenSocket socket3;
	final int PORT = 122;
	final int PORT2 = 80;
	final String HOST = "127.0.0.1";
	InetAddress address;
	InetAddress address2;

	final String HOST2 = "192.168.0.2";
	

//	final String HOST2 = "192.168.1.1";


	@Before
	public void setUp() throws UnknownHostException {
		address = InetAddress.getByName(HOST);
		socket2 = new OpenSocket(PORT);
		address2 = InetAddress.getByName(HOST2);
		socket3 = new OpenSocket(PORT2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void OpenSocketConstructorTest() {
		socket = new OpenSocket(65535 + 1);
		socket = new OpenSocket(1 - 1);

	}

	@Test
	public void getStatusTest() {
		assertEquals(Status.DOWN, socket2.getStatus(address));
		// only works in a Network where a device with the IP HOST2 is available
		//assertEquals(Status.UP, socket3.getStatus(address2));
	}

	@After
	public void tearDown() {
		socket2 = null;
		socket3 = null;
		address = null;
		address2 = null;

	}

}
