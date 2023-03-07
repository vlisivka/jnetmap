package ch.rakudave.jnetmap.model;

import java.util.EventListener;

public interface CurrentMapListener extends EventListener {
    /**
     * Called when a different map is displayed
     *
     * @see Map
     */
    void mapChanged(Map map);
}
