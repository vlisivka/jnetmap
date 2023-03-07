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
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.*;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Properties;
import java.util.function.Consumer;

public class MailNotifierPlugin extends JNetMapPlugin {
    private static Icon icon = Icons.get("mail");
    private static String pluginName = "Mail Notifier";

    public MailNotifierPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    @XStreamAlias("ch.rakudave.jnetmap.plugins.MailNotifier")
    public static class MailNotifier implements Notifier {
        private String name = "My MailNotifier", host, username, password, from, to;
        private int port = 587;
        private boolean useSSL = true;
        private DeviceEventFilter filter = new DeviceEventFilter();

        @Override
        public void statusChanged(DeviceEvent e, Map m) {
            try {
            boolean match = filter.matches(e);
            Logger.debug("Attempting to send mail, filtered: " + !match);
            if (!match) return;
                StringBuffer sb = new StringBuffer();
                if (Type.INTERFACE_STATUS_CHANGED.equals(e.getType())) {
                    formatIF(sb, (NetworkIF) e.getSubject());
                    sb.append("\n").append(Lang.getNoHTML("details")).append(":\n");
                }
                Device d = (Device) e.getSource();
                sb.append(Lang.getNoHTML("device"));
                sb.append(" \"").append(d.getName()).append("\"").append(": ");
                sb.append(d.getStatus().getMessage()).append("\n");
                for (NetworkIF nif : d.getInterfaces()) {
                    sb.append("\t");
                    formatIF(sb, nif);
                }
                Exception result = sendMessage(from, to, m.getFileName(), sb.toString());
                if (result != null) throw result;
            } catch (Exception ex) {
                Logger.warn("Failed to handle mail notification", ex);
            }
        }

        private void formatIF(StringBuffer sb, NetworkIF nif) {
            if (sb == null || nif == null) return;
            sb.append(Lang.getNoHTML("interface"));
            sb.append(" \"").append(nif.getName()).append("\"");
            if (nif.getAddress() != null) sb.append(" ").append(nif.getAddress());
            sb.append(": ").append(nif.getStatus().getMessage()).append("\n");
        }

        private Exception sendMessage(String from, String to, String subject, String body) {
            try {
                Message message = new MimeMessage(getSession());
                message.addFrom(new InternetAddress[]{new InternetAddress(from)});
                message.addRecipient(RecipientType.TO, new InternetAddress(to));
                message.setSubject("jNetMap - " + subject);
                message.setContent(body, "text/plain");
                Transport.send(message);
                return null;
            } catch (Exception ex) {
                Logger.error("Failed to send mail from " + from + " to " + to, ex);
                return ex;
            }
        }

        @Override
        public void showPropertiesWindow(Frame owner, boolean isSetup) {
            final JDialog d = new JDialog(owner, getPluginName() + " - " + name, ModalityType.DOCUMENT_MODAL);
            d.setMinimumSize(new Dimension(300, 530));
            d.setLayout(new BorderLayout(5, 5));
            final JTextField nameField = new JTextField(name);
            JPanel smtpWrapper = new JPanel(new GridLayout(0, 2, 5, 5));
            smtpWrapper.setBorder(BorderFactory.createTitledBorder("SMTP"));
            final JTextField hostField = new JTextField(host);
            final JSpinner portNr = new JSpinner(new SpinnerNumberModel(port, 0, 65535, 1));
            portNr.setToolTipText("25, 465, 587");
            final JCheckBox ssl = new JCheckBox("SSL", useSSL);
            ssl.addActionListener(e -> {
                useSSL = ssl.isSelected();
                portNr.setValue(useSSL ? 587 : 25);
            });
            final JTextField usernameField = new JTextField(username);
            final JPasswordField pwField = new JPasswordField(password);
            final JTextField fromField = new JTextField(from);
            usernameField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {}

                @Override
                public void focusLost(FocusEvent e) {
                    if (fromField.getText().isEmpty() && usernameField.getText().contains("@")) {
                        fromField.setText(usernameField.getText());
                    }
                }
            });
            final JTextField toField = new JTextField(to);
            smtpWrapper.add(new JLabel("Host:"));
            smtpWrapper.add(hostField);
            smtpWrapper.add(ssl);
            smtpWrapper.add(portNr);
            smtpWrapper.add(new JLabel("Username:"));
            smtpWrapper.add(usernameField);
            smtpWrapper.add(new JLabel("Password:"));
            smtpWrapper.add(pwField);
            JPanel mailWrapper = new JPanel(new GridLayout(0, 2, 5, 5));
            mailWrapper.setBorder(BorderFactory.createTitledBorder("Mail"));
            mailWrapper.add(new JLabel("From:"));
            mailWrapper.add(fromField);
            mailWrapper.add(new JLabel("To:"));
            mailWrapper.add(toField);
            mailWrapper.add(new JLabel(""));
            Consumer<ActionEvent> store = (e) -> {
                name = nameField.getText();
                host = hostField.getText();
                port = (Integer) portNr.getValue();
                username = usernameField.getText();
                password = new String(pwField.getPassword());
                from = fromField.getText();
                to = toField.getText();
                if (host.isEmpty() || username.isEmpty() || password.isEmpty() || from.isEmpty() || to.isEmpty()) {
                    JOptionPane.showMessageDialog(d, "Host, Username, Password, From and To may not be empty!",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (name.isEmpty()) name = "Mail to " + to;
            };
            JButton test = new JButton("Test");
            test.addActionListener(e -> {
                store.accept(e);
                Exception result = sendMessage(from, to, "Test", "Test");
                if (result != null) JOptionPane.showMessageDialog(d, result.getMessage());
                else JOptionPane.showMessageDialog(d, "Sent!");
            });
            mailWrapper.add(test);
            JPanel centerWrapper = new JPanel();
            centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.PAGE_AXIS));
            centerWrapper.add(smtpWrapper);
            centerWrapper.add(mailWrapper);
            centerWrapper.add(filter.settingsPanel());
            JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            JButton cancel = new JButton(Lang.get("action.cancel"), Icons.get("cancel"));
            cancel.addActionListener(e -> d.dispose());
            JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
            ok.setPreferredSize(cancel.getPreferredSize());
            ok.addActionListener(e -> {
                store.accept(e);
                d.dispose();
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
            return new MailNotifier();
        }

        @SuppressWarnings("serial")
        private Session getSession() {
            Properties props = new Properties() {{
                setProperty("mail.transport.protocol", "smtp");
                setProperty("mail.host", host);
                setProperty("mail.smtp.auth", "true");
                setProperty("mail.smtp.port", String.valueOf(port));
                setProperty("mail.user", username);
                setProperty("mail.password", password);
            }};
            if (useSSL) props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            return Session.getInstance(props, new Authenticator());
        }

        private class Authenticator extends javax.mail.Authenticator {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isUseSSL() {
            return useSSL;
        }

        public void setUseSSL(boolean useSSL) {
            this.useSSL = useSSL;
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
        return "Sends mails when a device/interface changes its status";
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