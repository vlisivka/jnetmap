package ch.rakudave.jnetmap.net;

import ch.rakudave.jnetmap.util.IO;
import ch.rakudave.jnetmap.util.logging.Logger;

import java.util.Properties;

public class OUI {
    private static OUI instance;
    private static final Object lock = new Object();
    private Properties ouiList;

    public static OUI getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new OUI();
                }
            }
        }
        return instance;
    }

    private OUI() {
        ouiList = IO.getMergedProps("/OUI");
    }

    public String lookup(String macAddress) {
        if (macAddress == null || macAddress.isEmpty() || macAddress.length() < 7) return "";
        try {
            macAddress = macAddress.toUpperCase().replaceAll("-", ":").replaceAll(" ", ":");
            String match = ouiList.getProperty(macAddress.length() == 7 ? macAddress : macAddress.substring(0, 8));
            if (match != null) return match;
            // some OUIs appear to be longer than the traditional first half, so:
            if (macAddress.length() > 12) {
                match = ouiList.getProperty(macAddress.substring(0, 13) + "0:00");
                if (match != null) return match;
            }
        } catch (Exception e) {
            Logger.debug("Failed to lookup OUI", e);
        }
        return "";
    }
}
