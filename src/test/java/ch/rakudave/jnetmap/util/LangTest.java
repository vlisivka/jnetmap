package ch.rakudave.jnetmap.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LangTest {
	String eng = "English";
	String deu = "Deutsch";
	String[] languages = { "Deutsch", "English" };
	String[] badLanguages = { "Deutsch", "Afrikaans" };
	String html = "<text>A &amp; O</text>";
	String no_html = "A & O";

	@Test
	public void getTest() {
		assertEquals(eng, Lang.get(eng));

	}

	@Test
	public void getNoHTMLTest() {
		//assertEquals(Lang.getNoHTML(html), no_html);
	}

	@Test
	public void loadTest() {
		Lang.load(eng);
		assertEquals(Lang.currentLanguage(), eng);
		Lang.load();
		assertEquals(Lang.currentLanguage(), eng);
		Lang.load(deu);
		assertEquals(Lang.currentLanguage(), deu);
		Lang.load();
		assertEquals(Lang.currentLanguage(), deu);
	}

}
