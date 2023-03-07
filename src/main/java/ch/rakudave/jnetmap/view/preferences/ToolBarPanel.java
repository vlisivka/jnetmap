package ch.rakudave.jnetmap.view.preferences;

import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.components.ToolBar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author rakudave
 */
@SuppressWarnings("serial")
public class ToolBarPanel extends PreferencePanel {
    @SuppressWarnings("rawtypes")
    private JList active, inactive;
    private JCheckBox showLabels;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ToolBarPanel() {
        title = Lang.getNoHTML("preferences.toolbar");
        active = new JList();
        inactive = new JList();
        setLayout(new BorderLayout());
        fillLists();
        showLabels = new JCheckBox(Lang.get("preferences.toolbar.labels"), Settings.getBoolean("toolbar.labels", false));
        JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        pane.setDividerLocation(180);
        pane.setDividerSize(0);
        JPanel left = new JPanel(new BorderLayout());
        active.setCellRenderer(new CellRenderer());
        active.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        //active.setDragEnabled(true);
        MouseAdapter listener = new ReorderListener();
        active.addMouseListener(listener);
        active.addMouseMotionListener(listener);
        left.add(new JLabel(Lang.get("preferences.toolbar.active")), BorderLayout.NORTH);
        left.add(active, BorderLayout.CENTER);
        pane.setLeftComponent(left);
        JPanel right = new JPanel(new BorderLayout());
        JPanel movers = new JPanel();
        movers.setLayout(new BoxLayout(movers, BoxLayout.Y_AXIS));
        JButton moveLeft = new JButton(new AbstractAction("", Icons.get("left")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                int inactiveSelectedItem = inactive.getSelectedIndex(), activeSelectedItem = active.getSelectedIndex();
                if (inactiveSelectedItem == -1) return;
                if (activeSelectedItem == -1) ++activeSelectedItem;
                DefaultListModel inactiveModel = ((DefaultListModel) inactive.getModel()), activeModel = ((DefaultListModel) active.getModel());
                Object obj = inactiveModel.getElementAt(inactiveSelectedItem);
                activeModel.add(activeSelectedItem, obj);
                if (obj instanceof ListItem && !"|".equals(((ListItem) obj).id) && !"-".equals(((ListItem) obj).id))
                    inactiveModel.remove(inactiveSelectedItem);
                active.setSelectedIndex(activeSelectedItem);
            }
        });
        JButton moveRight = new JButton(new AbstractAction("", Icons.get("right")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                int inactiveSelectedItem = inactive.getSelectedIndex(), activeSelectedItem = active.getSelectedIndex();
                if (activeSelectedItem == -1) return;
                if (inactiveSelectedItem == -1) ++inactiveSelectedItem;
                DefaultListModel inactiveModel = ((DefaultListModel) inactive.getModel()), activeModel = ((DefaultListModel) active.getModel());
                Object obj = activeModel.getElementAt(activeSelectedItem);
                activeModel.remove(activeSelectedItem);
                if (obj instanceof ListItem && !"|".equals(((ListItem) obj).id) && !"-".equals(((ListItem) obj).id))
                    inactiveModel.add(inactiveSelectedItem, obj);
                inactive.setSelectedIndex(inactiveSelectedItem);
            }
        });
        movers.add(Box.createVerticalGlue());
        movers.add(moveLeft);
        movers.add(moveRight);
        movers.add(Box.createVerticalGlue());
        inactive.setCellRenderer(new CellRenderer());
        inactive.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        JPanel label = new JPanel();
        label.setLayout(new BoxLayout(label, BoxLayout.X_AXIS));
        label.add(Box.createHorizontalStrut(28));
        label.add(new JLabel(Lang.get("preferences.toolbar.inactive")));
        right.add(label, BorderLayout.NORTH);
        right.add(movers, BorderLayout.WEST);
        right.add(inactive, BorderLayout.CENTER);
        pane.setRightComponent(right);
        add(showLabels, BorderLayout.NORTH);
        add(pane, BorderLayout.CENTER);
    }

    @Override
    @SuppressWarnings({"rawtypes"})
    public void save() {
        Settings.put("toolbar.layout", active.getModel().toString().replaceAll("[\\[\\] ]", ""));
        Settings.put("toolbar.labels", showLabels.isSelected());
        ToolBar.getInstance().rebuildToolbarLayout();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fillLists() {
        HashMap<String, Action> actions = ToolBar.getInstance().getActions();
        List<String> layout = Arrays.asList(Settings.get("toolbar.layout", ToolBar.defaultLayout).split(","));
        DefaultListModel model = new DefaultListModel();
        for (String s : layout) {
            try {
                if ("|".equals(s)) {
                    model.addElement(new ListItem("|", null));
                } else if ("-".equals(s)) {
                    model.addElement(new ListItem("-", null));
                } else {
                    model.addElement(new ListItem(s, actions.get(s)));
                    actions.remove(s);
                }
            } catch (Exception e) {
                Logger.error("Unable to resolve the toolbar-action " + s);
            }
        }
        active.setModel(model);
        model = new DefaultListModel();
        for (String s : actions.keySet()) {
            model.addElement(new ListItem(s, actions.get(s)));
        }
        model.addElement(new ListItem("|", null));
        model.addElement(new ListItem("-", null));
        inactive.setModel(model);
    }

    @SuppressWarnings({"rawtypes"})
    private class CellRenderer implements ListCellRenderer {
        private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof ListItem) {
                ListItem item = (ListItem) value;
                JLabel label = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText(item.text);
                label.setIcon(item.icon);
                return label;
            }
            return new JLabel(value.toString());
        }
    }

    private class ListItem {
        public String id, text;
        public ImageIcon icon;

        public ListItem(String id, Action a) {
            this.id = id;
            if (a != null) {
                this.text = (String) a.getValue(Action.NAME);
                this.icon = (ImageIcon) a.getValue(Action.SMALL_ICON);
            } else if ("|".equals(id)) {
                this.text = "-----";
            } else if ("-".equals(id)) {
                this.text = " <-> ";
            }
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private class ReorderListener extends MouseAdapter {
        private int pressIndex = 0;
        private int releaseIndex = 0;
        private Cursor grab = new Cursor(Cursor.MOVE_CURSOR);

        public ReorderListener() {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            pressIndex = active.locationToIndex(e.getPoint());
            active.setCursor(grab);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            releaseIndex = active.locationToIndex(e.getPoint());
            if (releaseIndex != pressIndex && releaseIndex != -1) {
                reorder();
            }
            active.setCursor(null);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            mouseReleased(e);
            pressIndex = releaseIndex;
            active.setCursor(grab);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void reorder() {
            DefaultListModel model = (DefaultListModel) active.getModel();
            Object dragee = model.elementAt(pressIndex);
            model.removeElementAt(pressIndex);
            model.insertElementAt(dragee, releaseIndex);
        }
    }
}
