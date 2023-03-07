package ch.rakudave.jnetmap.plugins.extensions;

import ch.rakudave.jnetmap.model.device.Device;
import org.pf4j.ExtensionPoint;

import javax.swing.*;

/**
 * @author rakudave
 */
public interface RightClickAction extends ExtensionPoint {

    /**
     * This function will be executed when a user right-clicks a device and selects this plugin.
     *
     * @param d that device that was right-clicked, use this to extract the address etc.
     */
    void execute(Device d);

    String getName();
    Icon getIcon();
}
