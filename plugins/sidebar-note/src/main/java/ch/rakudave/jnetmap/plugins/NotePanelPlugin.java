package ch.rakudave.jnetmap.plugins;

import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.plugins.extensions.SidebarPlugin;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.view.components.InfoPanel;
import ch.rakudave.jnetmap.view.preferences.PreferencePanel;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;

/**
 * A sidebar-panel to store notes for devices
 *
 * @author rakudave
 */

public class NotePanelPlugin extends JNetMapPlugin {
    private static Icon icon = Icons.get("notes");

    public NotePanelPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    public static class NotePanel implements SidebarPlugin {
        private Device device;
        private JLabel title;
        private JTextArea text;

        @Override
        public void graphReleased(Device device, MouseEvent event) {
        }

        @Override
        public void graphPressed(Device device, MouseEvent event) {
        }

        @Override
        public void graphClicked(Device device, MouseEvent event) {
            if (this.device != null) this.device.setMetadata("notes", text.getText());
            this.device = device;
            title.setText(this.device.getName());
            InfoPanel.updateDeviceLabel(device, title);
            text.setText(this.device.getMetadata("notes"));
            text.setEnabled(true);
        }

        @Override
        public JPanel getPanel() {
            JPanel p = new JPanel(new BorderLayout(0, 5));
            p.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            title = new JLabel("-", JLabel.LEADING);
            title.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            text = new JTextArea();
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.addFocusListener(new FocusListener() {
                @Override
                public void focusLost(FocusEvent e) {
                    if (device != null) device.setMetadata("notes", text.getText());
                }

                @Override
                public void focusGained(FocusEvent e) {
                }
            });
            text.setEnabled(false);
            p.add(title, BorderLayout.NORTH);
            p.add(new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
            return p;
        }

        @Override
        public String getTabTitle() {
            return Lang.get("notepanel.title");
        }

        @Override
        public String getToolTip() {
            return "";
        }

        @Override
        public Icon getIcon() {
            return icon;
        }
    }

    @Override
    public String getPluginName() {
        return Lang.getNoHTML("notepanel.title") + " Panel";
    }

    @Override
    public String getAuthor() {
        return "rakudave";
    }

    @Override
    public String getDescription() {
        return "Enables you to store notes about individual devices";
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