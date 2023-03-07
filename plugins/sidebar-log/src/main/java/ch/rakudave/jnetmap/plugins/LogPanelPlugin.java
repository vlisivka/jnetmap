package ch.rakudave.jnetmap.plugins;

import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.plugins.extensions.SidebarPlugin;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Appender;
import ch.rakudave.jnetmap.util.logging.ListenerAppender;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.util.logging.Logger.Level;
import ch.rakudave.jnetmap.view.preferences.PreferencePanel;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

/**
 * @author rakudave
 */

public class LogPanelPlugin extends JNetMapPlugin {
    private static Icon icon = Icons.get("properties");

    public LogPanelPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    public static class LogPanel implements ChangeListener, SidebarPlugin {
        private JTextArea area;
        private Appender appender;

        @Override
        @SuppressWarnings({"rawtypes", "unchecked", "serial"})
        public JPanel getPanel() {
            JPanel p = new JPanel(new BorderLayout());
            area = new JTextArea();
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            DefaultCaret caret = (DefaultCaret) area.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // scroll to bottom
            appender = new ListenerAppender(Level.valueOf(Settings.get("plugin.logpanel.loglevel", "INFO")), this);
            Logger.addAppender(appender);
            JPanel settingsHolder = new JPanel(new GridLayout(1, 2, 5, 5));
            final JComboBox levelSelector = new JComboBox(Level.values());
            levelSelector.setSelectedItem(appender.getLevel());
            levelSelector.addActionListener(e -> {
                Level logLevel = (Level) levelSelector.getSelectedItem();
                appender.setLevel(logLevel);
                Settings.put("plugin.logpanel.loglevel", logLevel.toString());
            });
            JButton clear = new JButton(new AbstractAction(Lang.get("action.clear")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    area.setText("");
                }
            });
            settingsHolder.add(levelSelector);
            settingsHolder.add(clear);
            p.add(settingsHolder, BorderLayout.NORTH);
            p.add(new JScrollPane(area), BorderLayout.CENTER);
            return p;
        }

        /* (non-Javadoc)
         * @see ch.rakudave.jnetmap.plugins.ILogPanel#stateChanged(javax.swing.event.ChangeEvent)
         */
        @Override
        public void stateChanged(ChangeEvent e) {
            area.append(e.getSource().toString());
        }

        @Override
        public Icon getIcon() {
            return icon;
        }

        @Override
        public String getTabTitle() {
            return "Log";
        }

        @Override
        public String getToolTip() {
            return "";
        }

        @Override
        public void graphClicked(Device v, MouseEvent me) {
        }

        @Override
        public void graphPressed(Device v, MouseEvent me) {
        }

        @Override
        public void graphReleased(Device v, MouseEvent me) {
        }
    }

    @Override
    public String getAuthor() {
        return "rakudave";
    }

    @Override
    public String getDescription() {
        return "Provides convenient access to the system log";
    }

    @Override
    public String getPluginName() {
        return "Log Panel";
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public PreferencePanel getSettingsPanel() {
        return null;
    }

    @Override
    public boolean hasSettings() {
        return false;
    }
}
