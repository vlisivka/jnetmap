package ch.rakudave.jnetmap.util;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.util.logging.Logger;

import java.io.*;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rakudave
 */
public class Settings {
    private static Properties p;
    public static final File file = new File(IO.userDir, "preferences");

    public static String get(String key, String safeDefault) {
        if (p == null) load();
        String result = p.getProperty(key);
        if (result == null) {
            if (safeDefault != null) p.put(key, safeDefault);
            return safeDefault;
        } else {
            return result;
        }
    }

    public static boolean getBoolean(String key, boolean safeDefault) {
        String s = get(key, Boolean.toString(safeDefault));
        if ("true".equals(s.toLowerCase())) return true;
        else
            return !"false".equals(s.toLowerCase()) && safeDefault;
    }

    public static double getDouble(String key, double safeDefault) {
        try {
            return Double.parseDouble(get(key, Double.toString(safeDefault)));
        } catch (NumberFormatException e) {
            Logger.trace("Error parsing double " + key, e);
            return safeDefault;
        }
    }

    public static float getFloat(String key, float safeDefault) {
        try {
            return Float.parseFloat(get(key, Float.toString(safeDefault)));
        } catch (NumberFormatException e) {
            Logger.trace("Error parsing float " + key, e);
            return safeDefault;
        }
    }

    public static int getInt(String key, int safeDefault) {
        try {
            return Integer.parseInt(get(key, Integer.toString(safeDefault)));
        } catch (NumberFormatException e) {
            Logger.trace("Error parsing integer " + key, e);
            return safeDefault;
        }
    }

    public static synchronized void load() {
        Logger.info("Loading preferences");
        if (p == null) p = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            p.load(fis);
        } catch (IOException e) {
            Logger.error("Failed to open preferences. Restoring defaults...", e);
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                save();
            } catch (IOException e1) {
                Logger.error("Failed to create settings file");
            }
        }
    }

    public static void put(String key, boolean value) {
        put(key, Boolean.toString(value));
    }

    public static void put(String key, double value) {
        put(key, Double.toString(value));
    }

    public static void put(String key, int value) {
        if (p == null) load();
        put(key, Integer.toString(value));
    }

    public static void put(String key, String value) {
        p.setProperty(key, value);
    }

    public static void remove(String key) {
        p.remove(key);
    }

    public static void removeAll(String prefix) {
        Set<String> removable = p.keySet().stream().map(o -> o.toString()).filter(s -> s.startsWith(prefix)).collect(Collectors.toSet());
        Logger.debug("Removing props: "+String.join(", ", removable));
        removable.forEach(s -> p.remove(s));
    }

    public static void save() {
        if (p == null) return;
        Logger.info("Saving preferences");
        put("version", Controller.version);
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            p.store(os, "jNetMap Settings");
        } catch (IOException e) {
            Logger.error("Failed to save preferences", e);
        } finally {
            try {
                os.close();
            } catch (Exception e) {
            }
        }
    }

    private Settings() {
    }
}
