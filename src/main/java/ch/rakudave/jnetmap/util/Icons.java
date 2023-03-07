package ch.rakudave.jnetmap.util;

import ch.rakudave.jnetmap.util.logging.Logger;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author rakudave
 */
public class Icons {
    public static final String DEFAULT_ICONS = "Elementary", DEFAULT_DEVICE = "cisco-svg";
    private static Map<String, ImageIcon> icons;
    private static Map<String, ImageIcon> deviceIcons;
    private static Map<String, ImageIcon> customCache = new HashMap<>();
    private static String currentIconTheme = Settings.get("icon.theme", DEFAULT_ICONS);
    private static String currentDeviceTheme = Settings.get("device.theme", DEFAULT_DEVICE);

    public static String currentTheme() {
        return currentIconTheme;
    }

    public static String currentDeviceTheme() {
        return currentDeviceTheme;
    }

    public static ImageIcon get(String id) {
        if (icons == null) load();
        ImageIcon icon = icons.get(id);
        if (icon == null) {
            Logger.warn("No icon named " + id + " in theme " + currentIconTheme);
            icon = icons.get("jnetmap_small");
            if (icon == null) icon = new ImageIcon();
        }
        return icon;
    }

    public static ImageIcon getCisco(String id) {
        if (deviceIcons == null) load();
        ImageIcon icon = deviceIcons.get(id);
        if (icon == null) {
            Logger.warn("No icon named " + id + " from cisco");
            icon = deviceIcons.get("workstation");
            if (icon == null) icon = new ImageIcon();
        }
        return icon;
    }

    public static String[] getThemes() {
        return IO.listFiles(IO.iconsDirName, true).toArray(new String[0]);
    }

    public static String[] getDeviceThemes() {
        return IO.listFiles(IO.devicesDirName, true).toArray(new String[0]);
    }

    public static boolean importTheme(File zip) {
        return IO.unzip(zip, new File(IO.userDir, IO.iconsDirName));
    }

    public static boolean importDeviceTheme(File zip) {
        return IO.unzip(zip, new File(IO.userDir, IO.devicesDirName));
    }

    public static void load() {
        try {
            loadIcons(currentIconTheme);
            loadDevices(currentDeviceTheme);
        } catch (Exception e) {
            loadIcons(DEFAULT_ICONS);
            loadDevices(DEFAULT_DEVICE);
        }
    }

    public static synchronized void loadIcons(String theme) {
        String path = IO.iconsDirName + "/" + theme;
        Logger.info("Loading icon-theme: " + theme);
        icons = loadImageIcons(path);
        currentIconTheme = theme;
        Settings.put("icon.theme", theme);
    }

    public static synchronized void loadDevices(String theme) {
        String path = IO.devicesDirName + "/" + theme;
        Logger.info("Loading device-theme: " + path);
        deviceIcons = loadImageIcons(path);
        currentDeviceTheme = theme;
        Settings.put("device.theme", theme);
    }

    private static Map<String, ImageIcon> loadImageIcons(String path) {
        Map<String, ImageIcon> result = new Hashtable<>();
        try {
            Logger.info("Loading " + path);
            for (String file : IO.listFiles(path, false)) {
                result.put(file.replaceAll("\\.\\w{3}$", ""), loadImageIcon(path, file));
            }
        } catch (Exception e) {
            Logger.fatal("Unable to load icons from " + path, e);
        }
        return result;
    }

    public static ImageIcon loadImageIcon(String path, String icon) {
        try {
            File userIcon = new File(new File(IO.userDir, path), icon);
            boolean isSVG = icon.endsWith(".svg");
            if (userIcon.exists()) {
                if (isSVG) return new FlatSVGIcon(userIcon.toURI());
                return new ImageIcon(userIcon.getAbsolutePath());
            } else {
                URL url = IO.getResource(path + "/" + icon);
                if (isSVG) return new FlatSVGIcon(url.toURI());
                return new ImageIcon(url);
            }
        } catch (Exception e) {
            Logger.warn("Failed to load icon "+path+"/"+icon, e);
            try { // fallback to hardcoded error placeholder icon
                return new ImageIcon(Base64.decode("iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAxxJREFUOI1tkk1oXFUYht9z7j0z986dzCRp82PNTxvRdtJOtYNjiUIXWhSUIl20IgilYC2UWkQExQYVBFfFQhd1U6i7LtyIEhVUrFAQSxPTHzOd2Exi0zGTuSXpnbmT3HPPn4sSkbTv9uV7v+d7+Qgeolt7cqMGZL+M9Ou5yzen/+9Vdu/oEbY8Ck0Gtv5WepOuHzYA4do67j3/QoG4qcmpZ3MnzQFYU8Xcnhsjwz9ESVYxbvYjoaEBwF4fMFncNuJ0d6UyxafgDm5x/e9/Ojk5hw8TXd3Ke2xr2u5+lCz/8l3LKJx5aIC22JHMrp2erN6GmK+h45ndbluhCL0aQzaaiP06eBBWiuOlGwBgXypsPw1C3iH0/jUxYcp9ZBONZ//CxtEzMErh9ltvQDRXkBh6As3ZW6HQ+tTaQjtS6gvG3COPv/KSl8h6IBYsWV1AvLgM6dehOYdaiWCUBnFSCKoLjjG0tBZA9169OS24fH/u0u+teClAePkKVsszkCGH9OsQC1VoqaClRlxfxIZc3pawL/6czx8GAAsAhur18Y2ptleZ4/Q4mQxVQQNaaSS3DUP4PsKJCSihEN9dgpVw4HX3sua94MXXOjpTZA3l61x+L0u53+x4ruDyygyMMTDaQEt9n0AokLYsSHsnAv+uXFr0IxWLMRsAzm/e7EhDz/YP9iV53YfkAjDAwKnTgNKYOXECWmkksu0oXy+tKh6PGUpGD5avlSkAuHb2XKazoz+dcqmhDHA8SC4BQgFKoWIJxQXEcgOMJXSs8ePB8rUyAJDzW/IHKLUv9HZl0VyJwigSqSeHB5io1WCUhpXJgjgO9EoEGUs0NMN8rTZ/aPbqIAGMtS+76RyARiNc/bIVyc+MwdNpZvfYRoN6HurNWE/97augJZqc2EkOj7S4oFfSG65/GyxO/1fims727/q0r939oDedsCvLUbgUxlVjYT+BHjIGhwysl0EpM1L/+fad8cIDr8wVfq0G8bt3Gpwrab5Ks8yxw3MXIwAlAGOf9424luL7JLATAB4g+KRre9pjyQkC8/F7//xxYb2/Xv8Cp4aA8/QaBv0AAAAASUVORK5CYII="));
            } catch (IOException e1) {
                return new ImageIcon();
            }
        }
    }

    public static ImageIcon fromBase64(String s) {
        ImageIcon img = customCache.get(s);
        if (img == null) {
            try {
                img = new ImageIcon(Base64.decode(s));
                img.setDescription(s);
                customCache.put(s, img);
            } catch (IOException e) {
                Logger.error("Unable to read encoded image", e);
                img = getCisco("workstation");
            }
        }
        return img;
    }

    /**
     * Extracts a Base64-String from a PNG-File
     *
     * @param f png-file
     * @return base64
     */
    public static String getBase64(File f) {
        if (f != null) {
            byte[] b = getPNGBytes(f);
            if (b != null) {
                return Base64.encodeBytes(b);
            }
        }
        return null;
    }

    private static byte[] getPNGBytes(File f) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
        try {
            BufferedImage orig = ImageIO.read(f);
            BufferedImage resized = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            resized.getGraphics().drawImage(orig, 0, 0, 64, 64, null);
            ImageIO.write(resized, "png", baos);
            baos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            Logger.error("Unable to read image " + f, e);
            return null;
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
            }
        }
    }

    private Icons() {
    }
}
