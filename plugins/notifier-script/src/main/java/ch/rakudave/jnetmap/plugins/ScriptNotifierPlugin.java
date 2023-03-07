package ch.rakudave.jnetmap.plugins;

import ch.rakudave.jnetmap.controller.Scheduler;
import ch.rakudave.jnetmap.model.IF.PhysicalIF;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.DeviceEvent;
import ch.rakudave.jnetmap.model.device.DeviceEvent.Type;
import ch.rakudave.jnetmap.plugins.extensions.Notifier;
import ch.rakudave.jnetmap.util.*;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.preferences.PreferencePanel;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class ScriptNotifierPlugin extends JNetMapPlugin {
    private static Icon icon = Icons.get("properties");
    private static String pluginName = "Script Notifier";

    public ScriptNotifierPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    @XStreamAlias("ch.rakudave.jnetmap.plugins.ScriptNotifier")
    public static class ScriptNotifier implements Notifier {
        private String name = "My ScriptNotifier", command = "", args = "";
        private DeviceEventFilter filter = new DeviceEventFilter();

        @Override
        public void statusChanged(final DeviceEvent e, final Map m) {
            boolean match = (!command.isEmpty()) && filter.matches(e);
            Logger.debug("Attempting to execute script, filtered: " + !match);
            if (match) {
                Scheduler.execute(() -> {
                    Process process = null;
                    try {
                        String[] commandAndArgs = IO.splitCommandArgs(command, fillArgs(args, e, m));
                        Logger.trace("executing script: " + Arrays.toString(commandAndArgs));
                        process = Runtime.getRuntime().exec(commandAndArgs);
                        process.waitFor();
                    } catch (Exception ex) {
                        Logger.warn("Unable to execute script", ex);
                    } finally {
                        if (process != null) process.destroy();
                    }
                });
            }
        }

        private String fillArgs(String arguments, DeviceEvent e, Map m) {
            String args = arguments
                    .replaceAll("%e", e.getType().toString())
                    .replaceAll("%mn", m.getFileName())
                    .replaceAll("%mp", m.getFilePath());
            Device d;
            if (Type.STATUS_CHANGED.equals(e.getType())) {
                d = (Device) e.getSource();
            } else {
                PhysicalIF nif = (PhysicalIF) e.getSubject();
                d = nif.getDevice();
                args = args.replaceAll("%is", nif.getStatus().toString())
                        .replaceAll("%in", nif.getName())
                        .replaceAll("%ia", nif.getAddress().getHostAddress())
                        .replaceAll("%iu", nif.getSubnet().getInfo().getBroadcastAddress())
                        .replaceAll("%ig", nif.getGateway().getHostAddress())
                        .replaceAll("%im", nif.getMacAddress());
            }
            args = args.replaceAll("%ds", d.getStatus().toString())
                    .replaceAll("%dn", d.getName())
                    .replaceAll("%dl", d.getLocation())
                    .replaceAll("%dt", d.getType().toString());
            return args;
        }

        @SuppressWarnings("serial")
        @Override
        public void showPropertiesWindow(final Frame owner, boolean isSetup) {
            final JDialog d = new JDialog(owner, getPluginName() + " - " + name, ModalityType.DOCUMENT_MODAL);
            d.setLayout(new BorderLayout(5, 5));
            final JTextField nameField = new JTextField(name);
            JPanel centerWrapper = new JPanel();
            centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.PAGE_AXIS));
            JPanel scriptWrapper = new JPanel(new GridLayout(0, 1, 2, 2));
            scriptWrapper.setBorder(BorderFactory.createTitledBorder("Script"));
            JPanel scriptSelector = new JPanel(new BorderLayout());
            final JTextField scriptPath = new JTextField(command);
            JButton browse = new JButton(new AbstractAction(Lang.get("browse")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    scriptPath.setText(SwingHelper.openDialog(owner, null).getAbsolutePath());
                }
            });
            JLabel l = new JLabel("File: ");
            l.setPreferredSize(new Dimension(50, 30));
            scriptSelector.add(l, BorderLayout.WEST);
            scriptSelector.add(scriptPath, BorderLayout.CENTER);
            scriptSelector.add(browse, BorderLayout.EAST);
            JPanel argsWrapper = new JPanel(new BorderLayout());
            final JTextField argsField = new JTextField();
            l = new JLabel("Args: ");
            l.setPreferredSize(new Dimension(50, 30));
            argsWrapper.add(l, BorderLayout.WEST);
            argsWrapper.add(argsField, BorderLayout.CENTER);
            scriptWrapper.add(scriptSelector);
            scriptWrapper.add(argsWrapper);
            JTextPane legend = new JTextPane();
            legend.setFocusable(false);
            legend.setText(createLegend());
            JScrollPane sp = new JScrollPane(legend, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setPreferredSize(new Dimension(460, 200));
            sp.setBorder(BorderFactory.createTitledBorder("Args-" + Lang.getNoHTML("preferences.legend")));
            centerWrapper.add(scriptWrapper);
            centerWrapper.add(sp);
            centerWrapper.add(filter.settingsPanel());
            JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            JButton cancel = new JButton(Lang.get("action.cancel"), Icons.get("cancel"));
            cancel.addActionListener(e -> d.dispose());
            JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
            ok.setPreferredSize(cancel.getPreferredSize());
            ok.addActionListener(e -> {
                command = scriptPath.getText();
                args = argsField.getText();
                name = nameField.getText();
                d.dispose();
            });
            if (!isSetup)
                bottomRow.add(cancel);
            bottomRow.add(ok);
            d.add(nameField, BorderLayout.NORTH);
            d.add(centerWrapper, BorderLayout.CENTER);
            d.add(bottomRow, BorderLayout.SOUTH);
            d.pack();
            SwingHelper.centerTo(owner, d);
            d.setVisible(true);
        }

        private String createLegend() {
            String sb = "%e = " + Lang.getNoHTML("event.type") + " [STATUS_CHANGED | INTERFACE_STATUS_CHANGED]\n" +
                    Lang.getNoHTML("map") + ":\n" +
                    "  %mn = " + Lang.getNoHTML("map.name") + "\n" +
                    "  %mp = " + Lang.getNoHTML("menu.file") + "\n" +
                    Lang.getNoHTML("device") + ":" + "\n" +
                    "  %ds = " + Lang.getNoHTML("message.status") + "\n" +
                    "  %dn = " + Lang.getNoHTML("device.name") + "\n" +
                    "  %dl = " + Lang.getNoHTML("device.location") + "\n" +
                    "  %dt = " + Lang.getNoHTML("device.type") + "\n" +
                    Lang.getNoHTML("interface") + ": (if INTERFACE_STATUS_UP)\n" +
                    "  %is = " + Lang.getNoHTML("message.status") + "\n" +
                    "  %in = " + Lang.getNoHTML("interface.name") + "\n" +
                    "  %ia = " + Lang.getNoHTML("interface.address") + "\n" +
                    "  %iu = " + Lang.getNoHTML("interface.subnet") + "\n" +
                    "  %ig = " + Lang.getNoHTML("interface.gateway") + "\n" +
                    "  %im = " + Lang.getNoHTML("interface.mac");
            return sb;
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
            return new ScriptNotifier();
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
        return "Calls a script when a device/interface changes its status";
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
