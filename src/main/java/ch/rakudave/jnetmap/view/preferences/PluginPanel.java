package ch.rakudave.jnetmap.view.preferences;

import ch.rakudave.jnetmap.controller.Actions;
import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.plugins.JNetMapPlugin;
import ch.rakudave.jnetmap.util.IO;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;
import ch.rakudave.jnetmap.util.logging.Logger;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author rakudave
 */
@SuppressWarnings("serial")
public class PluginPanel extends PreferencePanel {
    private JPanel pluginList;

    public PluginPanel() {
        setLayout(new BorderLayout());
        title = Lang.getNoHTML("preferences.plugins");
        pluginList = new JPanel();
        pluginList.setLayout(new BoxLayout(pluginList, BoxLayout.PAGE_AXIS));
        List<PluginWrapper> plugins = Controller.getPluginManager().getPlugins();
        for (PluginWrapper wrapper : plugins) {
            if (wrapper.getPlugin() instanceof JNetMapPlugin) {
                pluginList.add(new PluginInfo(wrapper));
                pluginList.add(new JSeparator());
            }
        }
        pluginList.add(Box.createVerticalGlue());
        final PluginPanel _this = this;
        JPanel bottom = new JPanel(new BorderLayout());
        JButton add = SwingHelper.createAlignedButton(new AbstractAction(Lang.get("preferences.plugins.get"), Icons.get("down")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.openWebsite(Actions.URL_PLUGIN);
            }
        });
        JButton install = SwingHelper.createAlignedButton(new AbstractAction(Lang.get("action.install"), Icons.get("install")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                File f = SwingHelper.openDialog(_this, new FileNameExtensionFilter("Java Archive File", "jar"));
                if (f == null) return; // cancel
                try {
                    IO.copy(f, new File(new File(IO.userDir, IO.pluginDirName), f.getName()), null);
                    JOptionPane.showMessageDialog(_this, Lang.get("message.import.restart"),
                            f.getName(), JOptionPane.INFORMATION_MESSAGE);
                    Controller.getPluginManager().loadPlugins();
                    Controller.getPluginManager().getPlugins().stream()
                            .filter(w -> f.getName().contains(w.getPluginId()))
                            .forEach(w -> {
                                Controller.getPluginManager().enablePlugin(w.getPluginId());
                                pluginList.add(new PluginInfo(w));
                                pluginList.add(new JSeparator());
                            });
                } catch (Exception e1) {
                    Logger.error("Failed to install plugin " + f, e1);
                    JOptionPane.showMessageDialog(_this, Lang.get("message.import.error"),
                            f.getName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        bottom.add(add, BorderLayout.WEST);
        bottom.add(install, BorderLayout.EAST);
        JScrollPane scroller = new JScrollPane(pluginList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        add(scroller, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    @Override
    public void save() {
    }

    private class PluginInfo extends JPanel implements ActionListener {
        private PluginWrapper wrapper;
        JButton enableButton;

        public PluginInfo(PluginWrapper wrapper) {
            super(new BorderLayout(5, 5));
            this.wrapper = wrapper;
            final JPanel _this = this;
            JNetMapPlugin p = (JNetMapPlugin) wrapper.getPlugin();
            setPreferredSize(new Dimension(380, 105));
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            JPanel bottom = new JPanel();
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.LINE_AXIS));
            enableButton = new JButton(Lang.get("action.disable"), Icons.get("cancel"));
            if (PluginState.DISABLED.equals(wrapper.getPluginState())) {
                enableButton.setText(Lang.get("action.enable"));
                enableButton.setIcon(Icons.get("ok"));
            }
            enableButton.addActionListener(this);
            enableButton.setMaximumSize(new Dimension(50, 30));
            JButton deleteButton = new JButton(Lang.get("action.delete"), Icons.get("close"));
            deleteButton.addActionListener(actionEvent -> {
                int action = JOptionPane.showConfirmDialog(_this, Lang.get("message.confirm.delete").replaceAll("%name%", p.getPluginName()),
                        Lang.get("action.delete"), JOptionPane.OK_CANCEL_OPTION);
                if (action != 0) return;
                Controller.getPluginManager().deletePlugin(wrapper.getPluginId());
                _this.setEnabled(false);
                Arrays.stream(_this.getComponents()).forEach(c -> c.setEnabled(false));
                enableButton.setEnabled(false);
                deleteButton.setEnabled(false);
            });
            deleteButton.setMaximumSize(new Dimension(50, 30));
            bottom.add(new JLabel("by " + p.getAuthor()));
            bottom.add(Box.createHorizontalGlue());
            bottom.add(enableButton);
            bottom.add(deleteButton);
            JLabel title = new JLabel("<html>" + p.getPluginName() + " <span style=\"color: gray; text-align: right;\">(" +
                    wrapper.getPluginId() + " " +wrapper.getDescriptor().getVersion()+ ")</span></html>", p.getIcon(), JLabel.LEFT);
            JTextArea description = new JTextArea(p.getDescription());
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setFocusable(false);
            description.setBackground(new Color(getBackground().getRGB()));
            description.setFont(title.getFont().deriveFont(Font.PLAIN, (int) (title.getFont().getSize()*0.9)));
            add(title, BorderLayout.NORTH);
            add(description, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (PluginState.DISABLED.equals(wrapper.getPluginState())) {
                if (Controller.getPluginManager().enablePlugin(wrapper.getPluginId())) {
                    enableButton.setText(Lang.get("action.disable"));
                    enableButton.setIcon(Icons.get("cancel"));
                }
            } else {
                if (Controller.getPluginManager().disablePlugin(wrapper.getPluginId())) {
                    enableButton.setText(Lang.get("action.enable"));
                    enableButton.setIcon(Icons.get("ok"));
                }
            }
        }
    }
}
