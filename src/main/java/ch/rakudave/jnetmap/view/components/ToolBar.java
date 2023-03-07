package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.controller.Actions;
import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.controller.command.Command;
import ch.rakudave.jnetmap.controller.command.CommandHistory;
import ch.rakudave.jnetmap.controller.command.CommandListener;
import ch.rakudave.jnetmap.model.CurrentMapListener;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.view.jung.EditModeListener;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author rakudave
 */
@SuppressWarnings("serial")
public class ToolBar extends JToolBar implements CurrentMapListener, CommandListener, EditModeListener {
    public static final String defaultLayout = "new,open,save,|,undo,redo,|,map.pick,map.edit,map.transform,|,refresh,map.zoomin,map.zoomout,-,hamburger";
    private static final String identifierRegex = "[a-zA-Z\\.]*";
    private static ToolBar instance;
    private static final HashMap<String, Action> actions = new HashMap<>();
    private CommandHistory commandHistory;
    private JButton undo, redo, mode_pick, mode_edit, mode_transform;

    private ToolBar() {
        super(Settings.getInt("toolbar.orientation", 0));
        setVisible(Settings.getBoolean("toolbar.visible", true));
        actions.put("new", Actions.newMap(Lang.get("menu.file.new")));
        actions.put("open", Actions.open(Lang.get("menu.file.open")));
        actions.put("save", Actions.save());
        actions.put("saveas", Actions.saveAs());
        actions.put("doc", Actions.viewDoc(Lang.get("menu.help.doc")));
        actions.put("undo", Actions.undo());
        actions.put("redo", Actions.redo());
        actions.put("refresh", Actions.refresh());
    }

    /**
     * @return an instance of ToolBar
     */
    public static ToolBar getInstance() {
        if (instance == null) {
            synchronized (actions) {
                if (instance == null) {
                    instance = new ToolBar();
                    instance.rebuildToolbarLayout();
                    Controller.addCurrentMapListener(instance);
                    TabPanel.addEditModeListener(instance);
                }
            }
        }
        return instance;
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        Settings.put("toolbar.visible", aFlag);
    }

    @Override
    public void setOrientation(int o) {
        super.setOrientation(o);
        Settings.put("toolbar.orientation", o);
    }

    /**
     * Add an action to the toolbar (as a button).
     * It will only be displayed if the toolbar-settings say so.
     *
     * @param identifier Unique identifier, used in the toolbar-settings
     *                   It may only consist of letters and dots, matching [a-zA-Z\\.]*
     * @param action     Button-action
     * @return Success. If the identifier already exists or does not match
     * the expected format, it will not be added.
     */
    public boolean add(String identifier, Action action) {
        if (identifier == null || action == null ||
                actions.containsKey(identifier) ||
                !identifier.matches(identifierRegex)) {
            return false;
        } else {
            actions.put(identifier, action);
            rebuildToolbarLayout();
            return true;
        }
    }

    public void remove(String identifier) {
        actions.remove(identifier);
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Action> getActions() {
        return (HashMap<String, Action>) actions.clone();
    }

    /**
     * Rebuilds the toolbars layout based on the configuration-strings in toolbar.layout
     */
    public synchronized void rebuildToolbarLayout() {
        instance.removeAll();
        List<String> layout = Arrays.asList(Settings.get("toolbar.layout", defaultLayout).split(","));
        for (String id : layout) {
            if ("|".equals(id)) {
                instance.addSeparator();
            } else if ("-".equals(id)) {
                instance.add(Box.createHorizontalGlue());
            } else if (id.matches(identifierRegex) && layout.contains(id) && actions.containsKey(id)) {
                Action a = actions.get(id);
                JButton b = new JButton(a);
                if (!Settings.getBoolean("toolbar.labels", false)) {
                    b.setText("");
                    b.setToolTipText((String) a.getValue(Action.NAME));
                }
                switch (id) {
                    case "undo": undo = b; break;
                    case "redo": redo = b; break;
                    case "map.pick": mode_pick = b; break;
                    case "map.edit": mode_edit = b; break;
                    case "map.transform": mode_transform = b; break;
                }
                instance.add(b);
            }
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    @Override
    public void executed(Command c) {
        if (redo != null) redo.setEnabled(false);
        if (undo != null) undo.setEnabled(true);
    }

    @Override
    public void undone(Command command) {
        if (redo != null) redo.setEnabled(true);
        if (undo != null) undo.setEnabled(commandHistory.canUndo());
    }

    @Override
    public void redone(Command command) {
        if (redo != null) redo.setEnabled(commandHistory.canRedo());
        if (undo != null) undo.setEnabled(true);
    }

    @Override
    public void mapChanged(Map map) {
        if (commandHistory != null) commandHistory.removeCommandListener(this);
        commandHistory = map.getHistory();
        commandHistory.addCommandListener(this);
        if (redo != null) redo.setEnabled(commandHistory.canRedo());
        if (undo != null) undo.setEnabled(commandHistory.canUndo());
    }

    @Override
    public void editModeChanged(ModalGraphMouse.Mode mode) {
        mode_pick.setEnabled(!ModalGraphMouse.Mode.PICKING.equals(mode));
        mode_edit.setEnabled(!ModalGraphMouse.Mode.EDITING.equals(mode));
        mode_transform.setEnabled(!ModalGraphMouse.Mode.TRANSFORMING.equals(mode));
    }
}
