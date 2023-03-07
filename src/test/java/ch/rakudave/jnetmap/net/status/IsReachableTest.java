package ch.rakudave.jnetmap.net.status;

import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IsReachableTest {
	public static IsReachable instance;
	public static IsReachable instance2;

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
		instance = new IsReachable();
		address2 = InetAddress.getByName(IP2);
		instance2 = new IsReachable();

	}

	// @SuppressWarnings("static-access")
	// @Test(expected=NullPointerException.class)
	// public void getInstanceTest(){
	// instance.getInstance();
	// }
	@Test
	public void getStatusTest() throws java.io.FileNotFoundException {

		assertTrue(isEqual(instance.getStatus(address), Status.DOWN));
		assertTrue(isEqual(instance2.getStatus(address2), Status.UP));
		assertFalse(isEqual(instance.getStatus(address), Status.UP));
	}

	public boolean isEqual(Status status, Status status2) {
		return (status == status2);
	}

	@After
	public void tearDownn() {
		address = null;
		address2 = null;
		instance = null;
		instance2 = null;

	}
}
