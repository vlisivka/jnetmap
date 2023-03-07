package ch.rakudave.jnetmap.view.preferences;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.net.status.Status;
import ch.rakudave.jnetmap.util.*;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.jung.EdgeTransformers;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author rakudave
 */
@SuppressWarnings("serial")
public class AppearancePanel extends PreferencePanel {
    private Window owner;
    private Preferences prefs;
    @SuppressWarnings("rawtypes")
    private JComboBox lafSelector, iconSelector, deviceSelector, fontNameSelector, shapeSelector;
    private JPanel bgButton, selectedButton, upButton, downButton, unknownButton, notFoundButton;
    private JCheckBox deviceLabelName, deviceLabelDescription, deviceLabelLocation, deviceLabelVendor, bgTransparent,
            deviceLabelModel, deviceLabelIP, deviceLabelMAC, connectionLabelName, connectionLabelLatency, connectionLabelStatus;
    private SpinnerModel fontSizeSpinner;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AppearancePanel(Window parent, Preferences preferences) {
        owner = parent;
        prefs = preferences;
        title = Lang.getNoHTML("preferences.appearance");
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // look and feel
        JPanel lafWrapper = new JPanel(new GridLayout(1, 2, 5, 5)) {{
            setMaximumSize(new Dimension(9999, 25));
            setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("preferences.general.laf") + " " + Lang.getNoHTML("message.restart")));
            List<String> lafClassNames = Arrays.stream(UIManager.getInstalledLookAndFeels())
                    .map(LookAndFeelInfo::getClassName)
                    .collect(Collectors.toList());
            // flatLaf not listed in getInstalledLookAndFeels, and no list available? so this here:
            lafClassNames.add("com.formdev.flatlaf.FlatLightLaf");
            lafClassNames.add("com.formdev.flatlaf.FlatDarkLaf");
            lafClassNames.add("com.formdev.flatlaf.FlatIntelliJLaf");
            lafClassNames.add("com.formdev.flatlaf.FlatDarculaLaf");
            lafSelector = new JComboBox(lafClassNames.toArray());
            lafSelector.setRenderer(new LafListRenderer());
            lafSelector.setSelectedItem(Settings.get("laf.theme", UIManager.getSystemLookAndFeelClassName()));
            add(lafSelector);
            add(SwingHelper.createAlignedButton(new AbstractAction(Lang.get("action.reset"), Icons.get("undo")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    lafSelector.setSelectedItem(UIManager.getSystemLookAndFeelClassName());
                }
            }));
        }};
        // icons
        JPanel iconWrapper = new JPanel(new GridLayout(1, 2, 5, 5)) {{
            setMaximumSize(new Dimension(9999, 25));
            setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("preferences.general.iconstyle") + " " + Lang.getNoHTML("message.restart")));
            iconSelector = new JComboBox(Icons.getThemes());
            iconSelector.setRenderer(new IconListRenderer());
            iconSelector.setSelectedItem(Icons.currentTheme());
            add(iconSelector);
            add(SwingHelper.createAlignedButton(new AbstractAction(Lang.get("action.install"), Icons.get("install")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    File zip = SwingHelper.openDialog(owner, new FileNameExtensionFilter("ZIP-file", "zip"));
                    if (zip != null) {
                        if (Icons.importTheme(zip)) {
                            iconSelector.addItem(zip.getName().replace(".zip", ""));
                            Logger.info("Successfully imported icon-theme " + iconSelector.getSelectedItem());
                        } else {
                            Logger.error("Failed to import icon-theme " + zip.getName());
                            JOptionPane.showMessageDialog(owner, Lang.get("message.import.error"),
                                    zip.getName(), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }));
        }};
        // device icons
        JPanel deviceIconWrapper = new JPanel(new GridLayout(1, 2, 5, 5)) {{
            setMaximumSize(new Dimension(9999, 25));
            setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("preferences.general.devicestyle")));
            deviceSelector = new JComboBox(Icons.getDeviceThemes());
            deviceSelector.setRenderer(new DeviceIconListRenderer());
            deviceSelector.setSelectedItem(Icons.currentDeviceTheme());
            add(deviceSelector);
            add(SwingHelper.createAlignedButton(new AbstractAction(Lang.get("action.install"), Icons.get("install")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    File zip = SwingHelper.openDialog(owner, new FileNameExtensionFilter("ZIP-file", "zip"));
                    if (zip != null) {
                        if (Icons.importDeviceTheme(zip)) {
                            deviceSelector.addItem(zip.getName().replace(".zip", ""));
                            Logger.info("Successfully imported icon-theme " + deviceSelector.getSelectedItem());
                        } else {
                            Logger.error("Failed to import device-theme " + zip.getName());
                            JOptionPane.showMessageDialog(owner, Lang.get("message.import.error"),
                                    zip.getName(), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }));
        }};
        // font
        JPanel fontWrapper = new JPanel(new BorderLayout(5, 5)) {{
            setMaximumSize(new Dimension(9999, 25));
            setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("preferences.appearance.font")));
            String[] fonts = Stream.concat(Stream.of(""), Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()))
                    .sorted(String::compareTo).toArray(String[]::new);
            fontNameSelector = new JComboBox(fonts);
            fontNameSelector.setSelectedItem(Settings.get("view.font.name", ""));
            add(fontNameSelector, BorderLayout.CENTER);
            fontSizeSpinner = new SpinnerNumberModel(Settings.getInt("view.font.size", 14), 5, 50, 1);
            add(new JSpinner(fontSizeSpinner), BorderLayout.EAST);
        }};
        // colors
        JPanel colors = new JPanel(new GridLayout(4, 2, 5, 5));
        colors.setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("preferences.appearance.color")));
        colors.setMaximumSize(new Dimension(9999, 120));
        upButton = createColorPanel(Status.UP, null, null);
        downButton = createColorPanel(Status.DOWN, null, null);
        unknownButton = createColorPanel(Status.UNKNOWN, null, null);
        notFoundButton = createColorPanel(Status.NOT_FOUND, null, null);
        bgButton = createColorPanel(null, new Color(Settings.getInt("background.color", Color.white.getRGB())), Lang.get("preferences.appearance.background"));
        selectedButton = createColorPanel(null, new Color(Settings.getInt("edge.selected.color", Color.black.getRGB())), Lang.get("preferences.appearance.edge.selected"));
        bgTransparent = new JCheckBox(Lang.get("preferences.appearance.background.transparent"), Settings.getBoolean("background.transparent", true));
        colors.add(upButton);
        colors.add(createResetButton(upButton, Color.green));
        colors.add(notFoundButton);
        colors.add(createResetButton(notFoundButton, Color.orange));
        colors.add(downButton);
        colors.add(createResetButton(downButton, Color.red));
        colors.add(unknownButton);
        colors.add(createResetButton(unknownButton, Color.gray));
        colors.add(bgButton);
        colors.add(createResetButton(bgButton, Color.white));
        colors.add(selectedButton);
        colors.add(createResetButton(selectedButton, Color.black));
        colors.add(bgTransparent);
        // Device label
        JPanel deviceWrapper = new JPanel(new GridLayout(3, 3, 5, 5)) {{
            setMaximumSize(new Dimension(9999, 120));
            setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("preferences.appearance.device.label")));
            deviceLabelName = new JCheckBox(Lang.get("device.name"), Settings.getBoolean("device.label.name", true));
            deviceLabelDescription =
            deviceLabelLocation = new JCheckBox(Lang.get("device.location"), Settings.getBoolean("device.label.location", false));
            deviceLabelVendor = new JCheckBox(Lang.get("device.vendor"), Settings.getBoolean("device.label.vendor", false));
            deviceLabelModel = new JCheckBox(Lang.get("device.model"), Settings.getBoolean("device.label.model", false));
            deviceLabelIP = new JCheckBox(Lang.get("interface.address"), Settings.getBoolean("device.label.ip", false));
            deviceLabelMAC = new JCheckBox(Lang.get("interface.mac"), Settings.getBoolean("device.label.mac", false));
            add(deviceLabelName);
            add(deviceLabelDescription);
            add(deviceLabelLocation);
            add(deviceLabelVendor);
            add(deviceLabelModel);
            add(deviceLabelIP);
            add(deviceLabelMAC);
        }};
        // Connection
        JPanel shapeWrapper = new JPanel(new GridLayout(2, 2, 5, 5)) {{
            setMaximumSize(new Dimension(9999, 80));
            setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("connection")));
            shapeSelector = new JComboBox(EdgeTransformers.Shape.values());
            shapeSelector.setSelectedItem(EdgeTransformers.Shape.valueOf(Settings.get("edge.shape", "Quad")));
            connectionLabelName = new JCheckBox(Lang.get("connection.name"), Settings.getBoolean("connection.label.name", false));
            connectionLabelStatus = new JCheckBox(Lang.get("message.status"), Settings.getBoolean("connection.label.status", false));
            connectionLabelLatency = new JCheckBox(Lang.get("connection.latency"), Settings.getBoolean("connection.label.latency", false));
            add(shapeSelector);
            add(connectionLabelStatus);
            add(connectionLabelName);
            add(connectionLabelLatency);
        }};
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(lafWrapper);
        add(Box.createVerticalStrut(5));
        add(iconWrapper);
        add(Box.createVerticalStrut(5));
        add(deviceIconWrapper);
        add(Box.createVerticalStrut(5));
        add(fontWrapper);
        add(Box.createVerticalStrut(5));
        add(colors);
        add(Box.createVerticalStrut(5));
        add(deviceWrapper);
        add(Box.createVerticalStrut(5));
        add(shapeWrapper);
        add(Box.createVerticalGlue());
    }

    private JPanel createColorPanel(Status status, Color overrideColor, String overrideName) {
        JPanel panel = new JPanel();
        panel.setBackground(overrideColor != null ? overrideColor : status.getColor());
        panel.add(new JLabel(overrideName != null ? overrideName : status.getMessage()));
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.addMouseListener(new MouseListener() {
            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                Color initialBackground = panel.getBackground();
                Color background = JColorChooser.showDialog(owner, Lang.getNoHTML("preferences.appearance.color"), initialBackground);
                if (background != null) panel.setBackground(background);
            }
        });
        return panel;
    }

    private JButton createResetButton(final JComponent display, final Color color) {
        return new JButton(new AbstractAction(Lang.get("action.reset"), Icons.get("undo")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                display.setBackground(color);
            }
        });
    }

    @Override
    public void save() {
        Settings.put("icon.theme", iconSelector.getSelectedItem().toString());
        Settings.put("device.theme", deviceSelector.getSelectedItem().toString());
        Icons.loadDevices(deviceSelector.getSelectedItem().toString());
        String fontName = fontNameSelector.getSelectedItem().toString();
        Settings.put("view.font.name", fontName);
        Settings.put("view.font.size", fontSizeSpinner.getValue().toString());
        Status.UP.setColor(upButton.getBackground());
        Status.DOWN.setColor(downButton.getBackground());
        Status.UNKNOWN.setColor(unknownButton.getBackground());
        Status.NOT_FOUND.setColor(notFoundButton.getBackground());
        Settings.put("background.color", bgButton.getBackground().getRGB());
        Settings.put("edge.selected.color", selectedButton.getBackground().getRGB());
        Settings.put("background.transparent", bgTransparent.isSelected());
        Settings.put("device.label.name", deviceLabelName.isSelected());
        Settings.put("device.label.description", deviceLabelDescription.isSelected());
        Settings.put("device.label.location", deviceLabelLocation.isSelected());
        Settings.put("device.label.vendor", deviceLabelVendor.isSelected());
        Settings.put("device.label.model", deviceLabelModel.isSelected());
        Settings.put("device.label.ip", deviceLabelIP.isSelected());
        Settings.put("device.label.mac", deviceLabelMAC.isSelected());
        Settings.put("connection.label.name", connectionLabelName.isSelected());
        Settings.put("connection.label.status", connectionLabelStatus.isSelected());
        Settings.put("connection.label.latency", connectionLabelLatency.isSelected());
        Settings.put("edge.shape", shapeSelector.getSelectedItem().toString());
        try {
            UIManager.setLookAndFeel(lafSelector.getSelectedItem().toString());
            Settings.put("laf.theme", lafSelector.getSelectedItem().toString());
            SwingHelper.setUIFont(new FontUIResource(fontName, Font.PLAIN, Settings.getInt("view.font.size", 14)));
            SwingUtilities.updateComponentTreeUI(prefs);
            SwingUtilities.updateComponentTreeUI(owner);
        } catch (Exception e) {
            Logger.debug("Failed to set L&F", e);
        }
        Controller.refreshAll();
    }

    @SuppressWarnings({"rawtypes"})
    private class IconListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);
            if (value == null) return label;
            try {
                String theme = (String) value;
                label.setIcon(Icons.loadImageIcon(IO.iconsDirName + "/" + theme, "jnetmap_small.png"));
            } catch (Exception e) {
                Logger.trace("Unable to get icon", e);
            }
            return label;
        }
    }

    @SuppressWarnings({"rawtypes"})
    private class DeviceIconListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);
            if (value == null) return label;
            try {
                String theme = (String) value;
                label.setIcon(Icons.loadImageIcon(IO.devicesDirName + "/" + theme, "workstation_small.png"));
            } catch (Exception e) {
                Logger.trace("Unable to get icon", e);
            }
            return label;
        }
    }

    private HashMap<Object, LookAndFeel> lafLookup = new HashMap<>();

    @SuppressWarnings({"rawtypes"})
    private class LafListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == null) return label;
            try {
                LookAndFeel lnf = lafLookup.get(value);
                if (lnf == null) {
                    Class lnfClass = Class.forName((String) value, true, Thread.currentThread().getContextClassLoader());
                    lnf = (LookAndFeel) lnfClass.newInstance();
                    lafLookup.put(value, lnf);
                }
                label.setText(" " + lnf.getName());
                label.setToolTipText(lnf.getDescription());
            } catch (Throwable e) {
                Logger.warn("Unable to load L&F", e);
            }
            return label;
        }
    }
}
