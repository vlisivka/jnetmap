package ch.rakudave.jnetmap.net.status;

import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;

public class NativePingTest {

	public static NativePing instance;
	InetAddress address;
	InetAddress address2;
	String IP = "192.138.0.0";
	String IP2 = "127.0.0.1";

	@BeforeClass
	public static void beforeClass() {
		Lang.load("English");
		Settings.load();
	}

	@Before
	public void setUp() throws UnknownHostException {
		address = InetAddress.getByName(IP);
		address2 = InetAddress.getByName(IP2);
		instance = new NativePing();
	}

	@Test
	public void getStatusTest() throws UnknownHostException {
		// does not work in WIndows
		assertEquals(Status.UP, instance.getStatus(address2));
		assertEquals(Status.DOWN, instance.getStatus(address));

	}

	@After
	public void tearDown() {
		address = null;
		address2 = null;
		instance = null;

	}

}
