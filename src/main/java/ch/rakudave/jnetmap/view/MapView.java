package ch.rakudave.jnetmap.view;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.controller.Listeners;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.plugins.extensions.SidebarPlugin;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.components.*;
import ch.rakudave.jnetmap.view.components.MenuBar;

import javax.swing.*;
import java.awt.*;

/**
 * Implements an IView for the JUNG Framework, has Tabs
 *
 * @author rakudave
 */
@SuppressWarnings("serial")
public class MapView extends JFrame implements IView, ISidebar {
    private JTabbedPane sideBar;
    private TabPanel mapPanel;
    private JSplitPane splitPane;
    private IStatusbar statusBar;

    public MapView() {
        super("jNetMap");
        Logger.info("Loading GUI");
        setLayout(new BorderLayout());
        if (Settings.getBoolean("mapview.remember.wh", true)) {
            setPreferredSize(new Dimension(Settings.getInt("mapview.geom.w", 750), Settings.getInt("mapview.geom.h", 550)));
        } else {
            setPreferredSize(new Dimension(750, 550));
        }
        if (Settings.getBoolean("mapview.remember.xy", true)) {
            setLocation(Settings.getInt("mapview.geom.x", 10), Settings.getInt("mapview.geom.y", 10));
        } else {
            setLocation(10, 10);
        }
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setIconImage(Icons.get("jnetmap").getImage());
        addWindowListener(Listeners.windowQuitListener());
        // SplitPane (Map | Sidebar)
        mapPanel = new TabPanel(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT, this);
        sideBar = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, mapPanel, sideBar);
        splitPane.setDividerSize(3);
        setSidebarVisible(Settings.getBoolean("mapview.sidebar.visible", true));
        add(splitPane, BorderLayout.CENTER);
        // add default sidebar tabs
        InfoPanel info = new InfoPanel(this);
        mapPanel.addGraphListener(info);
        addSideTab(Lang.get("infopanel.title"), Icons.get("info"), info, Lang.get("infopanel.tooltip"));
        addSideTab(Lang.get("menu.edit"), Icons.get("cursor_edit"), new EditPanel(this), "");
        if (Settings.getBoolean("features.experimental", false)) {
            addSideTab(Lang.get("layer.tab"), Icons.get("folder"), new LayerPanel(this), "");
        }
        // load plugin sidebar tabs
        for (SidebarPlugin p : Controller.getPluginManager().getExtensions(SidebarPlugin.class)) {
            try {
                addSideTab(p.getTabTitle(), p.getIcon(), p.getPanel(), p.getToolTip());
                mapPanel.addGraphListener(p);
            } catch (Exception e) {
                Logger.error("Unable to load plugin " + p, e);
            }
        }
        // Toolbar
        add(ToolBar.getInstance(), Settings.get("toolbar.location", "North"));
        // Statusbar
        statusBar = StatusBar.getInstance();
        add((Component) statusBar, BorderLayout.SOUTH);
        // Menubar
        setJMenuBar(new MenuBar(this));
        pack();
        // apply view settings
        double sidebarRatio = Settings.getDouble("mapview.sidebar.ratio", 0.8);
        if (sidebarRatio < 0 || sidebarRatio > 1) sidebarRatio = 0.8;
        splitPane.setDividerLocation(sidebarRatio);
        splitPane.setResizeWeight(sidebarRatio);
        setExtendedState(Settings.getInt("view.extendedstate", Frame.NORMAL));
        setVisible(true);
    }

    @Override
    public void addSideTab(String title, Icon icon, Component tab, String tip) {
        sideBar.addTab(title, icon, tab, tip);
    }

    @Override
    public boolean isSidebarVisible() {
        return splitPane.getRightComponent() != null;
    }

    @Override
    public void openMap(Map map) {
        mapPanel.openTab(map);
    }

    @Override
    public void removeSideTab(JPanel tab) {
        sideBar.remove(tab);
    }

    @Override
    public void saveViewProperties() {
        try {
            // Geometry
            Settings.put("mapview.geom.x", getLocationOnScreen().x);
            Settings.put("mapview.geom.y", getLocationOnScreen().y);
            Settings.put("mapview.geom.w", getWidth());
            Settings.put("mapview.geom.h", getHeight());
            //Toolbar
            Container content = getContentPane();
            BorderLayout layout = (BorderLayout) content.getLayout();
            Component comps[] = content.getComponents();
            for (Component c : comps) {
                if (c instanceof JToolBar) {
                    Settings.put("toolbar.location", layout.getConstraints(c).toString());
                }
            }
            // Sidebar
            Settings.put("mapview.sidebar.visible", isSidebarVisible());
            if (isSidebarVisible()) {
                double ratio = (4.0 + splitPane.getDividerLocation()) / getWidth();
                Settings.put("mapview.sidebar.ratio", ratio);
            }
        } catch (Exception e) {
            Logger.warn("Failed to save view properties", e);
        }
    }

    @Override
    public void setSidebarVisible(boolean visible) {
        splitPane.setRightComponent((visible) ? sideBar : null);
    }

    @Override
    public void setWindowTitle(String title) {
        super.setTitle("jNetMap - " + title);
    }
}