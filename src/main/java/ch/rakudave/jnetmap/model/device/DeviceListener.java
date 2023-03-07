package ch.rakudave.jnetmap.model.device;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.EventListener;

/**
 * Defines a listener that will be notified when a device changes its properties
 *
 * @author rakudave
 */
@XStreamAlias("DeviceListener")
public interface DeviceListener extends EventListener {
    /**
     * Called when a device changes its properties
     *
     * @param e event containing details of the change
     * @see DeviceEvent
     */
    void deviceChanged(DeviceEvent e);
}
