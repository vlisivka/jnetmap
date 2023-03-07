package ch.rakudave.jnetmap.plugins;

import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.DeviceEvent;
import ch.rakudave.jnetmap.model.device.DeviceEvent.Type;
import ch.rakudave.jnetmap.net.status.Status;
import ch.rakudave.jnetmap.plugins.extensions.Notifier;
import ch.rakudave.jnetmap.util.*;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.preferences.PreferencePanel;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAliasType;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

public class NotifyOsdPlugin extends JNetMapPlugin {
    private static Icon icon = Icons.fromBase64("iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH3wcVECohxX8A4QAAAUtJREFUOMu1UrFKA0EQfTPkioggHBYiESwVwcLKRoMExOrgWNJEay39A69U8BesrOz2Tk5IrY2ilb8ggqVgzKGuMzaXcAlJTBCnejO782be2wX+GDSsHobhlqrWiGgOwCuA2yzLLpvN5sdIAmPMinPunJnX+s9U9ZmIDqy1VwMJjDGrzrlrZp4ZtrKICDPvWmsvegiq1WrJ9/1HAMtjSG+LyFKSJE/cqfi+H47ZDABTRHQIAFzQt1248AZAf8E7PQQAFnKNX1mWVVT1uIMBnBRxPrACAKWum0QtAGBmr1wun4rI5ggMZn7vIVDVByIyebrP3F1uGL7v9+Blkh+oqmf9HjQmaLZxHKddgiAI5gHUROQbwF3B6UGROuf2OgkDgOd5s0R0xMyL1tp1ItoAkIhIK3+ZTwA3qtqw1gZpmrbH1lqv16ejKGL8V/wActKaehq+nl0AAAAASUVORK5CYII=");
    private static String pluginName = "NotifyOSD Notifier";

    public NotifyOsdPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    @XStreamAlias("ch.rakudave.jnetmap.plugins.UbuntuNotifier")
    public static class NotifyOsdNotifier implements Notifier {
        private String name = "My NotifyOSD";
        private DeviceEventFilter filter = new DeviceEventFilter();

        @Override
        public void statusChanged(DeviceEvent e, Map m) {
            boolean match = filter.matches(e);
            Logger.debug("Attempting to send ubuntu notification, filtered: " + !match);
            if (!match) return;
            Device d = e.getItem();
            StringBuffer sb = new StringBuffer();
            if (Type.INTERFACE_STATUS_CHANGED.equals(e.getType())) {
                NetworkIF nif = (NetworkIF) e.getSubject();
                sb.append(nif.getName()).append(" ");
                sb.append(nif.getAddress()).append(": ");
                sb.append(nif.getStatus().getMessage());
                libnotify(new String[]{"notify-send", "-i", "/usr/share/jnetmap/jnetmap.png", sb.toString(), Lang.get("device") + ": " + d.getName()});
            } else {
                for (Iterator<NetworkIF> it = d.getInterfaces().iterator(); it.hasNext(); ) {
                    formatIF(sb, it.next());
                    if (it.hasNext()) sb.append("\n");
                }
                libnotify(new String[]{"notify-send", "-i", "/usr/share/jnetmap/jnetmap.png", d.getName() + ": " + d.getStatus().getMessage(), sb.toString()});
            }
        }

        private void formatIF(StringBuffer sb, NetworkIF nif) {
            if (sb == null || nif == null) return;
            sb.append(nif.getName()).append(" ");
            sb.append(nif.getAddress()).append(": ");
            sb.append(nif.getStatus().getMessage());
        }

        public boolean libnotify(String[] commandAndArgs) {
            Logger.trace("notify-send command: " + Arrays.toString(commandAndArgs));
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(commandAndArgs);
                return process.waitFor() == 0;
            } catch (Exception ex) {
                Logger.warn("Unable to execute notify-send");
                return false;
            } finally {
                if (process != null) process.destroy();
            }
        }

        @SuppressWarnings("serial")
        @Override
        public void showPropertiesWindow(Frame owner, boolean isSetup) {
            final JDialog d = new JDialog(owner, getPluginName() + " - " + name, ModalityType.DOCUMENT_MODAL);
            d.setLayout(new BorderLayout(5, 5));
            final JTextField nameField = new JTextField(name);
            JPanel notifyWrapper = new JPanel(new GridLayout(0, 2, 5, 5));
            JLabel notifyCheck = new JLabel();
            JButton notifyTest = new JButton(new AbstractAction(Lang.get("action.test")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    libnotify(new String[]{"notify-send", "-i", "/usr/share/jnetmap/jnetmap.png",
                            "test_device: " + Status.UP.getMessage(), "eth0 /127.0.0.1: " + Status.UP.getMessage()});
                }
            });
            if (libnotify(new String[]{"notify-send", "--version"})) {
                notifyCheck.setText("libnotify-bin is installed");
                notifyCheck.setIcon(Icons.get("ok"));
            } else {
                notifyCheck.setText("libnotify-bin not installed");
                notifyCheck.setIcon(Icons.get("cancel"));
                notifyTest.setEnabled(false);
            }
            notifyWrapper.add(notifyCheck);
            notifyWrapper.add(notifyTest);
            JPanel centerWrapper = new JPanel();
            centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.PAGE_AXIS));
            centerWrapper.add(notifyWrapper);
            centerWrapper.add(filter.settingsPanel());
            JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            JButton cancel = new JButton(Lang.get("action.cancel"), Icons.get("cancel"));
            cancel.addActionListener(e -> d.dispose());
            JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
            ok.setPreferredSize(cancel.getPreferredSize());
            ok.addActionListener(e -> {
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
            return new NotifyOsdNotifier();
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
        return "Shows a freedesktop.org notification when a device/interface changes its status";
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