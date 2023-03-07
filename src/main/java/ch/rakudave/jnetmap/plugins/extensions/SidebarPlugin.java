package ch.rakudave.jnetmap.plugins.extensions;

import ch.rakudave.jnetmap.model.device.Device;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;
import org.pf4j.ExtensionPoint;

import javax.swing.*;

/**
 * Defines a JNetMapPlugin that can be displayed in IViews that implements ISidebar
 *
 * @author rakudave
 */
public interface SidebarPlugin extends ExtensionPoint, GraphMouseListener<Device> {
    /**
     * @return the JPanel that should be displayed in the ISideBar
     */
    JPanel getPanel();

    /**
     * @return the title that should be displayed in the tabs-component in the ISideBar
     */
    String getTabTitle();

    /**
     * @return the tooltip that should be displayed in the tabs-component in the ISideBar
     */
    String getToolTip();

    Icon getIcon();
}