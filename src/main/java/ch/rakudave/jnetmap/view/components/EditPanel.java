package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.controller.command.Command;
import ch.rakudave.jnetmap.controller.command.CommandHistory;
import ch.rakudave.jnetmap.controller.command.CommandListener;
import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.CurrentMapListener;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.device.Host;
import ch.rakudave.jnetmap.model.factories.ConnectionFactory;
import ch.rakudave.jnetmap.model.factories.DeviceFactory;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.Tuple;
import ch.rakudave.jnetmap.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;


@SuppressWarnings({"unchecked", "rawtypes", "serial"})
public class EditPanel extends JPanel implements CurrentMapListener, CommandListener {
    private JList icons;
    private JComboBox connectionType;
    private JTextField bandwidth;
    private Vector<Tuple<String, ImageIcon>> types;
    private JList<Command> history;
    private int historyIndex;
    private CommandHistory commandHistory;

    /**
     * Set the default device-type, connection-type and -speed for the factories
     *
     * @param owner
     */
    public EditPanel(final Frame owner) {
        setLayout(new GridLayout(2, 1, 5, 5));
        JPanel iconPanel = new JPanel(new BorderLayout(5, 5));
        iconPanel.setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("command.add.device")));
        types = new Vector<>();
        for (String t : Host.defaultTypes) {
            if (!Host.otherType.endsWith(t)) types.add(new Tuple<>(t, Icons.getCisco(t.toLowerCase())));
        }
        for (int i = 0; i < Settings.getInt("type.custom.count", 0); i++) {
            types.add(new Tuple<>(Host.otherType, Icons.fromBase64(Settings.get("type.custom." + i, ""))));
        }
        icons = new JList(types);
        icons.setCellRenderer(new IconCellRenderer());
        icons.setSelectedValue(Host.fallbackType, true);
        icons.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        icons.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        icons.setVisibleRowCount(-1);
        icons.addListSelectionListener(listSelectionEvent -> {
            Tuple<String, ImageIcon> tuple = (Tuple<String, ImageIcon>) icons.getSelectedValue();
            DeviceFactory.setType(tuple.getFirst());
            if (Host.otherType.equals(tuple.getFirst())) DeviceFactory.setIcon(tuple.getSecond().getDescription());
            TabPanel.getEditingModeSetter().actionPerformed(null);
        });
        icons.setBackground(new Color(getBackground().getRGB()));
        JPanel bottom = new JPanel(new GridLayout(0, 2, 5, 5));
        connectionType = new JComboBox(Connection.Type.values());
        connectionType.setSelectedItem(Connection.Type.Ethernet);
        connectionType.addActionListener(actionEvent -> ConnectionFactory.setType((Connection.Type) connectionType.getSelectedItem()));
        JPanel inner = new JPanel(new GridLayout(0, 2, 5, 5));
        String defaultBandwidth = String.valueOf(Connection.DEFAULT_BANDWIDTH);
        bandwidth = new JTextField(defaultBandwidth);
        bandwidth.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    double d = Double.valueOf(bandwidth.getText());
                    ConnectionFactory.setSpeed(d);
                    Logger.trace("Default bandwidth set to " + d);
                } catch (Exception e2) {
                    Logger.trace("Unable to convert to double: " + bandwidth.getText(), e2);
                    bandwidth.setText(defaultBandwidth);
                }
            }
        });
        inner.add(bandwidth);
        inner.add(new JLabel("Mb/s"));
        bottom.add(connectionType);
        bottom.add(inner);
        iconPanel.add(new JScrollPane(icons), BorderLayout.CENTER);
        iconPanel.add(bottom, BorderLayout.SOUTH);
        //TODO not an optimal solution, clogs up preferences-file. consider using separate file and class
        /*JButton add = new JButton(Lang.get("action.add"), Icons.get("add"));
			add.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					File f = SwingHelper.openDialog(EditPanel.this, new FileNameExtensionFilter("PNG Image", "png"));
					if (f != null) {
						String base64 = Icons.getBase64(f);
						int count = Settings.getInt("type.custom.count", 0);
						Settings.put("type.custom."+count, base64);
						Settings.put("type.custom.count", count+1);
						Logger.debug("Adding custom device type: "+base64);
						types.add(new Tuple<String, ImageIcon>(Type.Other.toString(), Icons.fromBase64(base64)));
						DeviceFactory.setType(Type.Other); DeviceFactory.setIcon(base64);
						icons.setSelectedIndex(types.size()-1);
						TabPanel.getEditingModeSetter().actionPerformed(null);
						EditPanel.this.repaint();
					}
				}
			});
		add(add, BorderLayout.NORTH);*/
        history = new JList<>();
        history.setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("historypanel.title")));
        history.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        history.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                history.setEnabled(false);
                int selectedIndex = history.getSelectedIndex();
                Logger.error("history: "+ historyIndex);
                Logger.error("selected: "+selectedIndex);
                if (historyIndex < selectedIndex) {
                    for (int i = historyIndex; i < selectedIndex; i++) {
                        Logger.error("undo: "+i);
                        if (commandHistory.canUndo()) commandHistory.undo();
                    }
                } else if (historyIndex > selectedIndex) {
                    for (int i = historyIndex; i > selectedIndex; i--) {
                        Logger.error("redo: "+i);
                        if (commandHistory.canRedo()) commandHistory.redo();
                    }
                }
                historyIndex = selectedIndex;
                history.setEnabled(true);
            }
            @Override
            public void mousePressed(MouseEvent mouseEvent) {}
            @Override
            public void mouseReleased(MouseEvent mouseEvent) {}
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {}
            @Override
            public void mouseExited(MouseEvent mouseEvent) {}
        });
        Controller.addCurrentMapListener(this);
        add(iconPanel);
        add(new JScrollPane(history, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
    }

    private class IconCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);
            if (value == null) return label;
            label.setText(null);
            label.setToolTipText(((Tuple<String, ImageIcon>) value).getFirst());
            label.setIcon(((Tuple<String, ImageIcon>) value).getSecond());
            label.setHorizontalAlignment(CENTER);
            label.setVerticalAlignment(CENTER);
            return label;
        }
    }

    @Override
    public void mapChanged(Map map) {
        if (commandHistory != null) commandHistory.removeCommandListener(this);
        commandHistory = map.getHistory();
        commandHistory.addCommandListener(this);
        history.setListData(commandHistory.getCommands());
        history.setSelectedIndex(commandHistory.getRedoSize());
        historyIndex = commandHistory.getRedoSize();
    }

    @Override
    public void executed(Command command) {
        history.setListData(commandHistory.getCommands());
        history.setSelectedIndex(0);
        historyIndex = 0;
    }

    @Override
    public void undone(Command command) {
        if (commandHistory.canUndo()) {
            history.setSelectedIndex(commandHistory.getRedoSize());
            historyIndex = commandHistory.getRedoSize();
        } else {
            history.setSelectedIndices(new int[0]);
            historyIndex = 0;
        }
    }

    @Override
    public void redone(Command command) {
        history.setSelectedIndex(commandHistory.getRedoSize());
    }
}
