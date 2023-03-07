package ch.rakudave.jnetmap.view;

import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.util.Settings;


/**
 * @author rakudave
 */
public interface IView {

    /**
     * Called to dispose (i.e. close) this window
     */
    void dispose();

    /**
     * @return true if this view is visible
     */
    boolean isVisible();

    /**
     * Opens a Map to be represented in this view
     *
     * @param map
     */
    void openMap(Map map);

    /**
     * Will be called by the controller right before disposing the window.
     * Use this method to save all relevant view-settings.
     *
     * @see Settings
     */
    void saveViewProperties();

    /**
     * Show or hide this view (i.e. window)
     *
     * @param visible
     */
    void setVisible(boolean visible);

    /**
     * Set a new window title. This method must prepend "jNetMap - ".
     *
     * @param title
     */
    void setWindowTitle(String title);

    /**
     * Request that the window be put in front of everything else.
     * Used when another InstanceDetector passes files to this instance.
     */
    void toFront();
}
