package ch.rakudave.jnetmap.model.factories;

import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.Host;
import org.apache.commons.collections15.Factory;

/**
 * @author rakudave
 */
public class DeviceFactory implements Factory<Device> {
    private static String type = Host.fallbackType, base64Icon;

    public DeviceFactory() {
    }

    @Override
    public Device create() {
        Host h = new Host(type);
        if (Host.otherType.equals(type)) h.setOtherID(base64Icon);
        return h;
    }

    public static void setType(String t) {
        if (t != null) type = t;
    }

    public static void setIcon(String base64Icon) {
        DeviceFactory.base64Icon = base64Icon;
    }
}
