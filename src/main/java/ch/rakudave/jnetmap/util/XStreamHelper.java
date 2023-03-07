package ch.rakudave.jnetmap.util;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.IF.LogicalIF;
import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.IF.PhysicalIF;
import ch.rakudave.jnetmap.model.IF.TransparentIF;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.MapListener;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.DeviceListener;
import ch.rakudave.jnetmap.net.status.*;
import ch.rakudave.jnetmap.plugins.JNetMapPlugin;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.CompositeClassLoader;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.pf4j.PluginWrapper;

import java.util.Arrays;

/**
 * @author rakudave
 */
public class XStreamHelper {
    /**
     * @return XStream-Object with all relevant class-mappings
     */
    public static XStream getXStream() {
        XStream xs = new XStream();
        xs.addPermission(AnyTypePermission.ANY); // TODO actually make use of permissions
        CompositeClassLoader compositeClassLoader = new CompositeClassLoader();
        xs.setClassLoader(compositeClassLoader);
        xs.processAnnotations(Map.class);
        xs.processAnnotations(MapListener.class);
        xs.processAnnotations(Connection.class);
        xs.processAnnotations(Device.class);
        xs.processAnnotations(DeviceListener.class);
        xs.processAnnotations(NetworkIF.class);
        xs.processAnnotations(PhysicalIF.class);
        xs.processAnnotations(LogicalIF.class);
        xs.processAnnotations(TransparentIF.class);
        xs.processAnnotations(PingMethod.class);
        xs.processAnnotations(IsReachable.class);
        xs.processAnnotations(NativePing.class);
        xs.processAnnotations(OpenSocket.class);
        xs.processAnnotations(Status.class);
        xs.processAnnotations(Tuple.class);
        for (PluginWrapper wrapper : Controller.getPluginManager().getPlugins()) {
            if (wrapper.getPlugin() instanceof JNetMapPlugin) {
                JNetMapPlugin plugin = (JNetMapPlugin) wrapper.getPlugin();
                compositeClassLoader.add(plugin.getClass().getClassLoader());
                xs.processAnnotations(plugin.getClass());
                Arrays.stream(plugin.getClass().getDeclaredClasses()).forEach(xs::processAnnotations);
            }
        }
        return xs;
    }

}
