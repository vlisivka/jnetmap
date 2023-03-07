package ch.rakudave.jnetmap.util;

import ch.rakudave.jnetmap.util.logging.Logger;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Properties;

/**
 * @author rakudave
 */
public class Lang {
    private static Properties lang, fallback;
    private static String currentLanguage = Settings.get("lang", "English"), fallbackLanguage = "English";

    public static String currentLanguage() {
        return currentLanguage;
    }

    /**
     * @param key id
     * @return translated string (in the current language) of the key if not found
     */
    public static String get(String key) {
        if (lang == null) load();
        String value = lang.getProperty(key);
        if (value == null) {
            Logger.debug("No translation for " + key + " in " + currentLanguage);
            if (fallback != null) value = fallback.getProperty(key);
            if (value == null) {
                Logger.warn("No translation for " + key);
                return key;
            }
        }
        return value;
    }

    public static String[] getLanguages() {
        return IO.listFiles(IO.langDirName, false).toArray(new String[0]);
    }

    public static String getNoHTML(String key) {
        return Jsoup.parse(get(key)).text();
    }

    /**
     * Loads the preferred language or English if none was defined
     */
    public static void load() {
        load(currentLanguage);
    }

    /**
     * Load a specified language, looks for a file with the same name in the /lang directory
     *
     * @param language
     */
    public static synchronized void load(String language) {
        Logger.info("Loading language: " + language);
        if (fallback == null && !fallbackLanguage.equals(language)) {
            fallback = new Properties();
            load(fallbackLanguage);
            fallback = lang;
        }
        try {
            lang = IO.getMergedProps(IO.langDirName + "/" + language);
            for (Object key : lang.keySet()) {
                // Properties.load() thinks it's cool to use Latin-1. I don't.
                String value = new String(((String) lang.get(key)).getBytes("iso8859-1"), "utf-8");
                lang.put(key, "<html>" + value + "</html>");
            }
            currentLanguage = language;
            Settings.put("lang", currentLanguage);
        } catch (IOException e) {
            Logger.error("Failed to open language-file " + language, e);
        }
    }

    private Lang() {
    }
}
