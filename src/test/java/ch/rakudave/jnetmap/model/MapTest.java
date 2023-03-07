package ch.rakudave.jnetmap.model;

import ch.rakudave.jnetmap.util.Lang;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
/**
 * @author sebehuber
 *
 */
public class MapTest {
	private Map map;
	private File mapfile;

	@BeforeClass
	public static void beforeClass() {
		Lang.load("English");
	}

	@Before
	public void setUp() {
		map = new Map();
	}

	@After
	public void tearDown() {
		map = null;
	}

	@Test
	public void getFileNameTest() {
		assertEquals("New map", map.getFileName());
	}

	@Test
	public void getPathNameTest() {
		assertEquals("unsaved", map.getFilePath());
	}


	@Test
	public void setFileTest() throws IOException {
		mapfile = File.createTempFile("testmap", "jnm");
		assertTrue(map.setFile(mapfile));
		mapfile.delete();
	}
}
