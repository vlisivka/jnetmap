package ch.rakudave.jnetmap.plugins;

import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.DeviceEvent;
import ch.rakudave.jnetmap.plugins.extensions.Notifier;
import ch.rakudave.jnetmap.util.DeviceEventFilter;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.preferences.PreferencePanel;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.NickAlreadyInUseException;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class IRCNotifierPlugin extends JNetMapPlugin {
    private static Icon icon = Icons.fromBase64("iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QAAAAAAAD5Q7t/AAAACXBIWXMAAA3XAAAN1wFCKJt4AAAAB3RJTUUH1QYGDQU74KgDcgAAATNJREFUOMuVk0FOhDAYhd9Pm7BtIhL3E29h9B7GlcQDmDgZgzqLiRBg9ARGY+IRNC71ApyEoDArF2OpG0oqA4h/wqK07+t7/VuCUVESHAF4wviaowVQ/6koCRTvwuZ5DqUUlFK9W7uuCwDgURJcAVjoiXgZDnre3zvAZLLbjDmAxWzqjw4dL8MNAAAgy7JeERGBiOA4TjPeAAwJTcFogCluQ7STTkCXsO1An0VVKY/32R0D8c8vH/hflrsARquJm7mKogARQQgx2Er5LU8YZ3cAFIXR9bFl0f1s6qMsSymEYEOXaXtr59bzvLPGgn4D+jLFyxCfH8Xhy/Pru17EGKts25YA1mmafgFYm10gM9eqXJ0m8c1bPWcBkACq+lP1/18ApXNd+PNHAKwWsnpN1YJIM9IPjQyRoGSUxmsAAAAASUVORK5CYII=");
    private static String pluginName = "IRC Notifier";

    public IRCNotifierPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    @XStreamAlias("ch.rakudave.jnetmap.plugins.IRCNotifier")
    public static class IRCNotifier implements Notifier {
        private String name;
        private String nick;
        private String server;
        private String channel;
        private DeviceEventFilter filter;
        private IRCBot bot;

            private class IRCBot extends PircBotX {
                public IRCBot(String serverAddress, String channel, String nickname) {
                    super();
                    setName(nickname);
                    for (int i = 1; i <= 3; )
                        try {
                            connect(serverAddress);
                            joinChannel(channel);
                            return;
                        } catch (Exception e) {
                            Logger.error((new StringBuilder("Unable to join IRC-Channel ")).append(channel).append(" at ").append(server).toString(), e);
                            if (e instanceof NickAlreadyInUseException)
                                nickname = (new StringBuilder(String.valueOf(nickname))).append("_").toString();
                            i++;
                        }

                }
            }

            public IRCNotifier() {
                nick = "jNetMap";
                filter = new DeviceEventFilter(true, true);
            }

            @Override
            public String getPluginName() {
                return pluginName;
            }

            public void statusChanged(DeviceEvent e, Map m) {
                boolean match = filter.matches(e);
                Logger.debug((new StringBuilder("Attempting to create log-entry, filtered: ")).append(!match).toString());
                if (!match)
                    return;
                Device d = e.getItem();
                StringBuffer sb = new StringBuffer();
                if (ch.rakudave.jnetmap.model.device.DeviceEvent.Type.INTERFACE_STATUS_CHANGED.equals(e.getType())) {
                    NetworkIF nif = (NetworkIF) e.getSubject();
                    sb.append(nif.getName()).append(" ");
                    sb.append(nif.getAddress()).append(": ");
                    sb.append(nif.getStatus().getMessage());
                    sb.append(" (").append(d.getName()).append(")");
                } else {
                    sb.append(d.getName()).append(": ");
                    sb.append(d.getStatus().getMessage());
                }
                if (bot != null)
                    if (bot.isConnected())
                        bot.sendMessage(channel, sb.toString());
                    else
                        try {
                            bot.reconnect();
                            bot.sendMessage(channel, sb.toString());
                        } catch (Exception e2) {
                            Logger.error((new StringBuilder("Unable to reconect to IRC-Channel ")).append(channel).append(" at ").append(server).toString(), e2);
                        }
            }

            public void showPropertiesWindow(Frame owner, boolean isSetup) {
                final JDialog d = new JDialog(owner, (new StringBuilder(String.valueOf(getPluginName()))).append(" - ").append(name).toString(), java.awt.Dialog.ModalityType.DOCUMENT_MODAL);
                d.setLayout(new BorderLayout(5, 5));
                final JTextField nameField = new JTextField(name);
                JPanel ircWrapper = new JPanel(new BorderLayout(5, 5));
                ircWrapper.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                JPanel labelWrapper = new JPanel(new GridLayout(0, 1, 5, 5));
                labelWrapper.add(new JLabel("Server:"));
                labelWrapper.add(new JLabel("Channel:"));
                labelWrapper.add(new JLabel("Nickname: "));
                JPanel fieldWrapper = new JPanel(new GridLayout(0, 1, 5, 5));
                final JTextField serverField = new JTextField(server);
                final JTextField channelField = new JTextField(channel);
                final JTextField nickField = new JTextField(nick);
                fieldWrapper.add(serverField);
                fieldWrapper.add(channelField);
                fieldWrapper.add(nickField);
                ircWrapper.add(labelWrapper, "West");
                ircWrapper.add(fieldWrapper, "Center");
                JPanel centerWrapper = new JPanel();
                centerWrapper.setLayout(new BoxLayout(centerWrapper, 3));
                centerWrapper.add(ircWrapper);
                centerWrapper.add(filter.settingsPanel());
                JPanel bottomRow = new JPanel(new FlowLayout(2, 5, 5));
                JButton cancel = new JButton(Lang.get("action.cancel"), Icons.get("cancel"));
                cancel.addActionListener(e -> d.dispose());
                JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
                ok.setPreferredSize(cancel.getPreferredSize());
                ok.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        name = nameField.getText();
                        if (!serverField.getText().equals(server) || !channelField.getText().equals(channel) || !nickField.getText().equals(nick)) {
                            if (bot != null && bot.isConnected()) {
                                bot.disconnect();
                                bot.dispose();
                            }
                            server = serverField.getText();
                            channel = channelField.getText();
                            nick = nickField.getText();
                            bot = new ch.rakudave.jnetmap.plugins.IRCNotifierPlugin.IRCNotifier.IRCBot(server, channel, nick);
                        }
                        if (name.isEmpty())
                            name = (new StringBuilder(String.valueOf(server))).append(channel).toString();
                        d.dispose();
                    }
                });
                if (!isSetup)
                    bottomRow.add(cancel);
                bottomRow.add(ok);
                d.add(nameField, "North");
                d.add(centerWrapper, "Center");
                d.add(bottomRow, "South");
                d.pack();
                SwingHelper.centerTo(owner, d);
                d.setVisible(true);
            }

            public String getName() {
                return name;
            }

            public Icon getIcon() {
                return icon;
            }

            public Notifier create() {
                ch.rakudave.jnetmap.plugins.IRCNotifierPlugin.IRCNotifier n = new ch.rakudave.jnetmap.plugins.IRCNotifierPlugin.IRCNotifier();
                if (server != null && !server.isEmpty() && channel != null && !channel.isEmpty())
                    n.bot = new ch.rakudave.jnetmap.plugins.IRCNotifierPlugin.IRCNotifier.IRCBot(server, channel, nick);
                return n;
            }
        }

    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public String getAuthor() {
        return "rakudave using PircBot (jibble.org)";
    }

    @Override
    public String getDescription() {
        return "Posts events to an IRC-Channel";
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
