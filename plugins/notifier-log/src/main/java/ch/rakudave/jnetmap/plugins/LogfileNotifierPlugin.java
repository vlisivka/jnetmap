package ch.rakudave.jnetmap.plugins;

import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.DeviceEvent;
import ch.rakudave.jnetmap.model.device.DeviceEvent.Type;
import ch.rakudave.jnetmap.plugins.extensions.Notifier;
import ch.rakudave.jnetmap.util.DeviceEventFilter;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.preferences.PreferencePanel;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAliasType;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Date;

public class LogfileNotifierPlugin extends JNetMapPlugin {
    private static Icon icon = Icons.get("notes");
    private static String pluginName = "Logfile Notifier";

    public LogfileNotifierPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    @XStreamAlias("ch.rakudave.jnetmap.plugins.LogfileNotifier")
    public static class LogfileNotifier implements Notifier {
        private String name = "My LogNotifier";
        private DeviceEventFilter filter = new DeviceEventFilter(true, true);
        private File logfile;
        @XStreamOmitField
        private PrintWriter writer;

        @Override
        public void statusChanged(DeviceEvent e, Map m) {
            boolean match = filter.matches(e);
            Logger.debug("Attempting to create log-entry, filtered: " + !match);
            if (!match) return;
            Device d = e.getItem();
            StringBuilder sb = new StringBuilder();
            sb.append(new Date().toString()).append(" [");
            sb.append(m.getFileName()).append("] - ");
            if (Type.INTERFACE_STATUS_CHANGED.equals(e.getType())) {
                NetworkIF nif = (NetworkIF) e.getSubject();
                sb.append(nif.getName()).append(" ");
                sb.append(nif.getAddress()).append(": ");
                sb.append(nif.getStatus().getMessage());
                sb.append(" (").append(d.getName()).append(")");
            } else {
                sb.append(d.getName()).append(": ");
                sb.append(d.getStatus().getMessage());
            }
            if (writer != null) writer.println(sb.toString());
        }

        @SuppressWarnings("serial")
        @Override
        public void showPropertiesWindow(final Frame owner, boolean isSetup) {
            final JDialog d = new JDialog(owner, "Logfile Notifier - " + name, ModalityType.DOCUMENT_MODAL);
            d.setLayout(new BorderLayout(5, 5));
            final JTextField nameField = new JTextField(name);
            JPanel fileWrapper = new JPanel(new BorderLayout(5, 5));
            fileWrapper.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            final JLabel label = new JLabel(Lang.get("menu.file"));
            final JTextField file = new JTextField((logfile != null) ? logfile.getAbsolutePath() : "");
            final Color defaultColor = new Color(file.getForeground().getRGB());
            file.addFocusListener(new FocusListener() {
                @Override
                public void focusLost(FocusEvent e) {
                }

                @Override
                public void focusGained(FocusEvent e) {
                    label.setForeground(defaultColor);
                }
            });
            JButton browse = new JButton(new AbstractAction(Lang.get("action.browse")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    File f = SwingHelper.saveDialog(owner, null);
                    if (f != null) {
                        file.setText(f.getAbsolutePath());
                    }
                }
            });
            fileWrapper.add(label, BorderLayout.WEST);
            fileWrapper.add(file, BorderLayout.CENTER);
            fileWrapper.add(browse, BorderLayout.EAST);
            JPanel centerWrapper = new JPanel();
            centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.PAGE_AXIS));
            centerWrapper.add(fileWrapper);
            centerWrapper.add(filter.settingsPanel());
            JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            JButton cancel = new JButton(Lang.get("action.cancel"), Icons.get("cancel"));
            cancel.addActionListener(e -> d.dispose());
            JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
            ok.setPreferredSize(cancel.getPreferredSize());
            ok.addActionListener(e -> {
                name = nameField.getText();
                if (!file.getText().isEmpty()) {
                    logfile = new File(file.getText());
                    if (logfile.canWrite()) {
                        try {
                            writer = new PrintWriter(new FileOutputStream(logfile, true), true);
                            d.dispose();
                        } catch (FileNotFoundException e1) {
                            Logger.error("Failed to setup LogfileNotifier");
                            writer = null;
                            file.setText("");
                        }
                    } else {
                        file.setText("");
                    }
                }
                label.setForeground(Color.red);
            });
            if (!isSetup) bottomRow.add(cancel);
            bottomRow.add(ok);
            d.add(nameField, BorderLayout.NORTH);
            d.add(centerWrapper, BorderLayout.CENTER);
            d.add(bottomRow, BorderLayout.SOUTH);
            d.pack();
            SwingHelper.centerTo(owner, d);
            d.setVisible(true);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getPluginName() {
            return pluginName;
        }

        @Override
        public Icon getIcon() {
            return icon;
        }

        @Override
        public Notifier create() {
            return new LogfileNotifier();
        }
    }


    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public String getAuthor() {
        return "rakudave";
    }

    @Override
    public String getDescription() {
        return "Logs events to a file";
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    @Override
    public PreferencePanel getSettingsPanel() {
        return null;
    }

}
