package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.controller.Actions;
import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.controller.RecentlyOpened;
import ch.rakudave.jnetmap.controller.command.Command;
import ch.rakudave.jnetmap.controller.command.CommandHistory;
import ch.rakudave.jnetmap.controller.command.CommandListener;
import ch.rakudave.jnetmap.model.CurrentMapListener;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.util.*;
import ch.rakudave.jnetmap.view.ISidebar;
import ch.rakudave.jnetmap.view.IView;
import ch.rakudave.jnetmap.view.jung.EditModeListener;
import ch.rakudave.jnetmap.view.properties.MapProperties;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;

/**
 * @author rakudave
 */
@SuppressWarnings("serial")
public class MenuBar extends JMenuBar implements CurrentMapListener, CommandListener, EditModeListener {
    private static final long serialVersionUID = -1861842226695622060L;
    private IView parent;
    private JMenuItem undo, redo, mode_pick, mode_edit, mode_transform;
    private JPopupMenu popupMenu;
    private Action popupAction;
    private boolean hidden;
    private CommandHistory commandHistory;

    public MenuBar(IView parent) {
        super();
        this.parent = parent;
        add(createFileMenu());
        add(createEditMenu());
        add(createViewMenu());
        add(createToolMenu());
        add(createHelpMenu());

        popupMenu = new JPopupMenu();
        popupMenu.add(createFileMenu());
        popupMenu.add(createEditMenu());
        popupMenu.add(createViewMenu());
        popupMenu.add(createToolMenu());
        popupMenu.add(createHelpMenu());
        popupAction = new AbstractAction(Lang.getNoHTML("menu"), Icons.get("hamburger")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton b = (JButton) e.getSource();
                popupMenu.show(b, (int) b.getAlignmentX(), (int) b.getAlignmentY() + b.getHeight());
            }
        };

        setVisible(Settings.getBoolean("menu.visible", true));
        Controller.addCurrentMapListener(this);
        TabPanel.addEditModeListener(this);
    }

    private JMenu createFileMenu() {
        JMenu menu = new JMenu(Lang.getNoHTML("menu.file"));
        // New
        JMenuItem i = new JMenuItem(Actions.newMap(Lang.getNoHTML("menu.file.new")));
        menu.add(i);
        i.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        // Open
        i = new JMenuItem(Actions.open(Lang.getNoHTML("menu.file.open")));
        menu.add(i);
        i.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        // Recent
        JMenu recent = new JMenu(Lang.getNoHTML("menu.file.recent"));
        recent.setIcon(Icons.get("recent"));
        recent.addMenuListener(new RecentlyOpened(recent));
        menu.add(recent);
        menu.add(new JSeparator());
        // Save
        i = new JMenuItem(Actions.save());
        menu.add(i);
        i.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        // Save As
        i = new JMenuItem(Actions.saveAs());
        menu.add(i);
        i.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        // Export
        i = new JMenuItem(new AbstractAction(Lang.getNoHTML("menu.file.exportimg"), Icons.get("export-img")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                File f = SwingHelper.saveDialog(null, new FileNameExtensionFilter("Portable Network Graphics", "png"));
                if (f == null || TabPanel.getCurrentTab() == null) return;
                IO.exportImage(TabPanel.getCurrentTab().vv, f);
            }
        });
        menu.add(i);
        i.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        // Properties
        menu.add(new JSeparator());
        i = new JMenuItem(new AbstractAction(Lang.getNoHTML("menu.file.properties"), Icons.get("properties")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Controller.getCurrentMap() == null) return;
                new MapProperties((Frame) Controller.getView(), Controller.getCurrentMap());
            }
        });
        menu.add(i);
        i.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        menu.add(new JSeparator());
        // Restart
        i = new JMenuItem(Actions.restart(Lang.getNoHTML("menu.file.restart")));
        menu.add(i);
        // Quit
        i = new JMenuItem(Actions.quit(Lang.getNoHTML("menu.file.quit")));
        menu.add(i);
        i.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        return menu;
    }

    private JMenu createEditMenu() {
        JMenu menu = new JMenu(Lang.getNoHTML("menu.edit"));
        // Undo/Redo
        undo = new JMenuItem(Actions.undo());
        menu.add(undo);
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        redo = new JMenuItem(Actions.redo());
        menu.add(redo);
        redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        menu.add(new JSeparator());
        // GraphMouse mode selection
        mode_pick = new JMenuItem(TabPanel.getPickingModeSetter());
        menu.add(mode_pick);
        mode_pick.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        mode_edit = new JMenuItem(TabPanel.getEditingModeSetter());
        menu.add(mode_edit);
        mode_edit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        mode_transform = new JMenuItem(TabPanel.getTransformingModeSetter());
        menu.add(mode_transform);
        mode_transform.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        menu.add(new JSeparator());
        // Preferences
        JMenuItem i = new JMenuItem(Actions.preferences(Lang.getNoHTML("menu.edit.preferences")));
        menu.add(i);
        i.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu(Lang.getNoHTML("menu.view"));
        // Refresh
        JMenuItem i = new JMenuItem(Actions.refresh());
        menu.add(i);
        i.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        // Layouts
        JMenu sub = new JMenu(Lang.getNoHTML("menu.view.layout"));
        for (Action a : TabPanel.getLayoutTransformerActions()) {
            sub.add(a);
        }
        menu.add(sub);
        // Zoom
        menu.add(new JSeparator());
        i = new JMenuItem(TabPanel.getZoomPlus());
        menu.add(i);
        i.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS/* | KeyEvent.VK_ADD*/, InputEvent.CTRL_DOWN_MASK));
        i = new JMenuItem(TabPanel.getZoomMinus());
        menu.add(i);
        i.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS/* | KeyEvent.VK_SUBTRACT*/, InputEvent.CTRL_DOWN_MASK));
        i = new JMenuItem(TabPanel.getZoomReset());
        menu.add(i);
        i.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0/* | KeyEvent.VK_NUMPAD0*/, InputEvent.CTRL_DOWN_MASK));
        menu.add(new JSeparator());
        // Toggle Toolbar
        final JMenuItem toggleToolbar = new JCheckBoxMenuItem(Lang.getNoHTML("menu.view.toolbar"), ToolBar.getInstance().isVisible());
        toggleToolbar.addActionListener(e -> ToolBar.getInstance().setVisible(toggleToolbar.isSelected()));
        toggleToolbar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        menu.add(toggleToolbar);
        // Toggle Sidebar
        if (parent instanceof ISidebar) {
            final JMenuItem toggleSidebar = new JCheckBoxMenuItem(Lang.getNoHTML("menu.view.sidebar"), ((ISidebar) parent).isSidebarVisible());
            toggleSidebar.addActionListener(e -> ((ISidebar) parent).setSidebarVisible(toggleSidebar.isSelected()));
            toggleSidebar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
            menu.add(toggleSidebar);
        }
        // Toggle Statusbar
        final JMenuItem toggleStatusbar = new JCheckBoxMenuItem(Lang.getNoHTML("menu.view.statusbar"), StatusBar.getInstance().isVisible());
        toggleStatusbar.addActionListener(e -> StatusBar.getInstance().setVisible(toggleStatusbar.isSelected()));
        toggleStatusbar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        menu.add(toggleStatusbar);
        // Toggle Menubar
        final MenuBar _this = this;
        final JMenuItem toggleMenubar = new JCheckBoxMenuItem(Lang.getNoHTML("menu.view.menubar"), isVisible());
        toggleMenubar.addActionListener(e -> _this.setVisible(toggleMenubar.isSelected()));
        toggleMenubar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        menu.add(toggleMenubar);
        return menu;
    }

    private JMenu createToolMenu() {
        JMenu menu = new JMenu(Lang.getNoHTML("menu.tools"));
        menu.add(new JMenuItem(new AbstractAction(Lang.getNoHTML("port.scanner"), Icons.get("find")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new PortScanner((Frame) parent, "127.0.0.1");
            }
        }));
        menu.add(new JMenuItem(new AbstractAction(Lang.getNoHTML("oui.lookup")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new OUILookup((Frame) parent);
            }
        }));
        menu.add(new JMenuItem(new AbstractAction(Lang.getNoHTML("network.scanner"), Icons.get("net")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Controller.getCurrentMap() == null) Actions.newMap("").actionPerformed(null);
                new NetworkScanner((Frame) parent, Controller.getCurrentMap());
            }
        }));
            /*menu.add(new JSeparator());
			menu.add(new JMenuItem(new AbstractAction(Lang.getNoHTML("menu.tools.report"), Icons.get("report")) {
				@Override
				public void actionPerformed(ActionEvent e) {
					//TODO Report.createReport();
				}
			}));*/
        return menu;
    }

    private JMenu createHelpMenu() {
        JMenu menu = new JMenu(Lang.getNoHTML("menu.help"));
        menu.add(new JMenuItem(Actions.viewDoc(Lang.getNoHTML("menu.help.doc"))));
        menu.add(new JSeparator());
        menu.add(new JMenuItem(Actions.viewWebsite()));
        menu.add(new JMenuItem(Actions.reportBug()));
        menu.add(new JMenuItem(Actions.requestFeature()));
        menu.add(new JMenuItem(Actions.contactDeveloper()));
        menu.add(new JSeparator());
        menu.add(new JMenuItem(new AbstractAction(Lang.getNoHTML("about"), Icons.get("info")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new AboutDialog((Frame) Controller.getView());
            }
        }));
        return menu;
    }

    @Override
    public void setVisible(boolean aFlag) {
        ToolBar toolbar = ToolBar.getInstance();
        Settings.put("menu.visible", aFlag);
        hidden = !aFlag;

        if (aFlag) {
            toolbar.remove("hamburger");
        } else {
            toolbar.add("hamburger", popupAction);
            // prevent menu- and toolbar being hidden at the same time (because then you can't do anything)
            if (!toolbar.isVisible()) toolbar.setVisible(true);
            String layout = Settings.get("toolbar.layout", ToolBar.defaultLayout);
            if (!layout.contains("hamburger")) {
                Settings.put("toolbar.layout", layout + ",-,hamburger");
            }
        }
        revalidate();
        toolbar.rebuildToolbarLayout();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        // fix for broken accelerators when hidden, see http://stackoverflow.com/a/18097498/3297862
        if (hidden) pref.height = 0;
        return pref;
    }

    @Override
    public void executed(Command c) {
        redo.setEnabled(false);
        undo.setEnabled(true);
    }

    @Override
    public void undone(Command command) {
        redo.setEnabled(true);
        undo.setEnabled(commandHistory.canUndo());
    }

    @Override
    public void redone(Command command) {
        redo.setEnabled(commandHistory.canRedo());
        undo.setEnabled(true);
    }

    @Override
    public void mapChanged(Map map) {
        if (commandHistory != null) commandHistory.removeCommandListener(this);
        commandHistory = map.getHistory();
        commandHistory.addCommandListener(this);
        redo.setEnabled(commandHistory.canRedo());
        undo.setEnabled(commandHistory.canUndo());
    }

    @Override
    public void editModeChanged(ModalGraphMouse.Mode mode) {
        mode_pick.setEnabled(!ModalGraphMouse.Mode.PICKING.equals(mode));
        mode_edit.setEnabled(!ModalGraphMouse.Mode.EDITING.equals(mode));
        mode_transform.setEnabled(!ModalGraphMouse.Mode.TRANSFORMING.equals(mode));
    }
}
