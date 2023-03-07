
/**
 * 
 * @author sebehuber
 *
 */

package ch.rakudave.jnetmap.util;


import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SettingsTest {
	static String key_string = "test.string";
	static String key_boolean = "test.boolean";
	static String key_int = "test.int";
	static String key_double = "test.double";

	@Test
	public void stringTest() {
		Settings.put(key_string, "TestString");
		assertEquals("TestString", Settings.get(key_string, "Nope"));
	}

	@Test
	public void booleanTest() {
		Settings.put(key_boolean, true);
		assertTrue(Settings.getBoolean(key_boolean, false));
	}

	@Test
	public void intTest() {
		Settings.put(key_int, 13);
		assertEquals(13, Settings.getInt(key_int, 699));
	}

	@Test
	public void doubleTest() {
		Settings.put(key_double, 4.12345678);
		assertEquals(4.12345678, Settings.getDouble(key_double, 0.00), 0.00001);
	}

	@Test
	public void saveTest() {
		Settings.save();
		Settings.remove(key_int);
		Settings.load();
		assertEquals(699, Settings.getInt(key_int, 699));
	}

	@AfterClass
	public static void tearDown() {
		Settings.remove(key_string);
		Settings.remove(key_boolean);
		Settings.remove(key_int);
		Settings.remove(key_double);
		Settings.save();
	}

}
