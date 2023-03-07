package ch.rakudave.jnetmap.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.EventListener;

/**
 * Defines a listener that will be notified when anything changes on the map
 *
 * @author rakudave
 */
@XStreamAlias("MapListener")
public interface MapListener extends EventListener {
    /**
     * Called when something changes on a map
     *
     * @param e event containing details of the change
     * @see MapEvent
     */
    void mapChanged(MapEvent e);
}
