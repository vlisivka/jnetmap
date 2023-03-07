package ch.rakudave.jnetmap.view;

import javax.swing.*;
import java.awt.*;

/**
 * @author rakudave
 */
public interface ISidebar {
    /**
     * Add a Tab to this sidebar
     *
     * @param title of the tab
     * @param icon  of the tab
     * @param tab   tab-component
     * @param tip   tooltip
     */
    void addSideTab(String title, Icon icon, Component tab, String tip);

    /**
     * @return visibility
     */
    boolean isSidebarVisible();

    /**
     * removes a previously added tab
     *
     * @param tab-component
     */
    void removeSideTab(JPanel tab);

    /**
     * Toggle sidebar visibility
     *
     * @param visible
     */
    void setSidebarVisible(boolean visible);
}
