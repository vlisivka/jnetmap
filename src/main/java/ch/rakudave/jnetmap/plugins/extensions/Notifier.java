package ch.rakudave.jnetmap.plugins.extensions;

import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.device.DeviceEvent;
import org.apache.commons.collections15.Factory;
import org.pf4j.ExtensionPoint;

import javax.swing.*;
import java.awt.*;

/**
 * A special MapListener designed for Notifiers.
 * Only notifies DeviceEvents of Type STATUS_CHANGED and INTERFACE_STATUS_CHANGED
 * Any fields of implementing classes that are not serializable with
 * xstream MUST use the @XStreamOmitField-annotation or saving a map will fail...
 *
 * @author rakudave
 */
public interface Notifier extends ExtensionPoint, Factory<Notifier> {

    /**
     * Notifies the plugin when the status of a device or an interface has changed
     *
     * @param e the associated DeviceEvent
     */
    void statusChanged(DeviceEvent e, Map m);

    /**
     * Shows a properties-window for this instance of the plugin,
     * for example to set the email-address that should be notified of the change
     *
     * @param owner   to be displayed on top of
     * @param isSetup true the first time after instance creation
     */
    void showPropertiesWindow(Frame owner, boolean isSetup);

    /**
     * @return Descriptive name of this instance, such as "Notifies some@one.com"
     */
    String getName();

    String getPluginName();

    Icon getIcon();
}
