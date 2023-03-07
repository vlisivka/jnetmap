package ch.rakudave.jnetmap.view.preferences;

import ch.rakudave.jnetmap.plugins.RightClickExec;
import ch.rakudave.jnetmap.plugins.extensions.RightClickAction;
import ch.rakudave.jnetmap.util.IO;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author rakudave
 */
@SuppressWarnings("serial")
public class ScriptsPanel extends PreferencePanel {
    private static boolean dirty;
    private DefaultTableModel tableModel;

    public ScriptsPanel() {
        title = Lang.getNoHTML("preferences.scripts");
        int count = Settings.getInt("plugin.exec.count", 0);
        String data[][] = new String[count][];
        for (int i = 0; i < count; i++) {
            data[i] = new String[]{
                    Settings.get("plugin.exec." + i + ".name", ""),
                    Settings.get("plugin.exec." + i + ".command", ""),
                    Settings.get("plugin.exec." + i + ".args", "")};
        }
        tableModel = new DefaultTableModel(data, new String[] {
                Lang.get("preferences.scripts.name"),
                Lang.get("preferences.scripts.command"),
                Lang.get("preferences.scripts.args")
        });
        final JTable table = new JTable(tableModel);
        // Buttons
        JPanel buttons = new JPanel(new GridLayout(1, 2));
        JButton addButton = new JButton(new AbstractAction(Lang.get("action.add"), Icons.get("add")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableModel.addRow(new String[3]);
                table.requestFocus();
                table.editCellAt(table.getRowCount()-1,0);
            }
        });
        JButton removeButton = new JButton(new AbstractAction(Lang.get("action.remove"), Icons.get("remove")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row > -1) tableModel.removeRow(row);
            }
        });
        buttons.add(addButton);
        buttons.add(removeButton);
        // Legend
        JTextPane legend = new JTextPane();
        legend.setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("preferences.scripts.args")));
        legend.setMaximumSize(new Dimension(500, 25));
        legend.setFocusable(false);
        legend.setContentType("text/html");
        legend.setBackground(new Color(getBackground().getRGB()));
        String sb = "<html><table width=\"100%\">" + "<tr><td>%n</td><td>" + Lang.getNoHTML("device.name") + "</td>" +
                "<td>%d</td><td>" + Lang.getNoHTML("device.description") + "</td></tr>" +
                "<tr><td>%a</td><td>" + Lang.getNoHTML("interface.address") + " (IP)" + "</td>" +
                "<td>%l</td><td>" + Lang.getNoHTML("device.location") + "</td></tr>" +
                "<tr><td>%h</td><td>" + Lang.getNoHTML("interface.address") + " (Host)" + "</td>" +
                "<td>%m</td><td>" + Lang.getNoHTML("device.model") + "</td></tr>" +
                "<tr><td>%s</td><td>" + Lang.getNoHTML("message.status") + "</td>" +
                "<td>%v</td><td>" + Lang.getNoHTML("device.vendor") + "</td></tr>" +
                "<tr><td>%t</td><td>" + Lang.getNoHTML("device.type") + "</td>" +
                "<td></td><td></td></tr></table></html>";
        legend.setText(sb);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        add(buttons);
        add(Box.createVerticalGlue());
        add(legend);
    }

    @Override
    public void save() {
        Settings.removeAll("plugin.exec");
        int count = 0;
        for (int i = 0; i < tableModel.getRowCount(); ++i) {
            try {
                Object c1 = tableModel.getValueAt(i, 0), c2 = tableModel.getValueAt(i, 1), c3 = tableModel.getValueAt(i, 2);
                if (c1 != null && c2 != null) {
                    Settings.put("plugin.exec." + count + ".name", c1.toString());
                    Settings.put("plugin.exec." + count + ".command", c2.toString());
                    if (c3 != null) Settings.put("plugin.exec." + count + ".args", c3.toString());
                    count++;
                } else {
                    Logger.warn("Skipped script on line " + i + " because name or command is missing");
                }
            } catch (Exception e) {
                Logger.warn("Failed to save script on line " + i, e);
            }
        }
        Settings.put("plugin.exec.count", count);
        dirty = true;
    }

    public static List<RightClickAction> getScriptPlugins() {
        if (!Settings.getBoolean("plugin.autoconfigured.exec", false)) autoconfigure();
        int count = Settings.getInt("plugin.exec.count", 0);
        List<RightClickAction> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            try {
                RightClickExec p = new RightClickExec(
                        Settings.get("plugin.exec." + i + ".name", ""),
                        Settings.get("plugin.exec." + i + ".command", ""),
                        Settings.get("plugin.exec." + i + ".args", ""));
                Logger.debug("Adding exec plugin " + p.getName());
                result.add(p);
            } catch (Exception e) {
                Logger.debug("Failed to add exec plugin #" + i);
            }
        }
        dirty = false;
        return result;
    }

    public static boolean isDirty() {
        return dirty;
    }

    private static void autoconfigure() {
        int count = Settings.getInt("plugin.exec.count", 0);
        if (IO.isLinux) {
            String terminal = null;
            if (commandExist("gnome-terminal", "--version")) {
                terminal = "gnome-terminal";
            } else if (commandExist("konsole", "--version")) {
                terminal = "konsole";
            } else if (commandExist("mate-terminal", "--version")) {
                terminal = "mate-terminal";
            } else if (commandExist("which", "x-terminal-emulator")) {
                terminal = "x-terminal-emulator";
            }
            if (terminal != null) {
                Logger.info("Autoconfigured "+terminal+" for SSH");
                Settings.put("plugin.exec." + count + ".name", "SSH ("+terminal+")");
                Settings.put("plugin.exec." + count + ".command", terminal);
                Settings.put("plugin.exec." + count + ".args", "-e \"ssh %a\"");
                count++;
            }
            if (commandExist("which", "remmina")) {
                Settings.put("plugin.exec." + count + ".name", "RDP (remmina)");
                Settings.put("plugin.exec." + count + ".command", "remmina");
                Settings.put("plugin.exec." + count + ".args", "-n -t rdp -s %a");
                count++;
            }
        } else {
            if (commandExist("where", "putty.exe")) {
                Settings.put("plugin.exec." + count + ".name", "SSH (putty)");
                Settings.put("plugin.exec." + count + ".command", "putty.exe");
                Settings.put("plugin.exec." + count + ".args", "-ssh %a");
                count++;
            }
            if (commandExist("where", "mstsc.exe")) {
                Settings.put("plugin.exec." + count + ".name", "RDP");
                Settings.put("plugin.exec." + count + ".command", "mstsc.exe");
                Settings.put("plugin.exec." + count + ".args", "/v:%a");
                count++;
            }
        }
        Settings.put("plugin.exec.count", count);
        Settings.put("plugin.autoconfigured.exec", true);
    }

    private static boolean commandExist(String... commandAndArgs) {
        Logger.trace("Running command: " + Arrays.toString(commandAndArgs));
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(commandAndArgs);
            return process.waitFor() == 0;
        } catch (Exception ex) {
            Logger.debug("Unable to execute: " + Arrays.toString(commandAndArgs));
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }
}
