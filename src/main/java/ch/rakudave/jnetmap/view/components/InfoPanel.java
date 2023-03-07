package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.controller.Scheduler;
import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.IF.PhysicalIF;
import ch.rakudave.jnetmap.model.IF.TransparentIF;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.Host;
import ch.rakudave.jnetmap.net.OUI;
import ch.rakudave.jnetmap.net.PortScan;
import ch.rakudave.jnetmap.net.SNMP;
import ch.rakudave.jnetmap.net.status.Status;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.Tuple;
import ch.rakudave.jnetmap.view.properties.ConnectionProperties;
import ch.rakudave.jnetmap.view.properties.DeviceProperties;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

@SuppressWarnings("serial")
public class InfoPanel extends JPanel implements GraphMouseListener<Device> {
    private Device current;
    private JLabel icon, status;
    private JTextArea description, history;
    private JList<Object> snmpLeft, snmpRight, interfaces;
    private JButton prefs, ports;

    /**
     * Context-sentitive sidebar-panel, show details of selected device
     *
     * @param owner
     */
    public InfoPanel(final Frame owner) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        infoPanel.setMaximumSize(new Dimension(2000, 135));
        icon = new JLabel("", Icons.getCisco("workstation"), JLabel.LEADING);
        icon.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        description = new JTextArea();
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFocusable(false);
        description.setBackground(new Color(getBackground().getRGB()));
        JScrollPane spDescription = new JScrollPane(description);
        spDescription.setViewportBorder(BorderFactory.createEmptyBorder());
        status = new JLabel();
        infoPanel.add(icon, BorderLayout.NORTH);
        infoPanel.add(spDescription, BorderLayout.CENTER);
        infoPanel.add(status, BorderLayout.SOUTH);
        JPanel nifPanel = new JPanel(new GridLayout(1, 1));
        nifPanel.setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("device.interfaces")));
        interfaces = new JList<>();
        interfaces.setCellRenderer(new IFListRenderer());
        interfaces.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    new ConnectionProperties(owner, ((NetworkIF) interfaces.getSelectedValue()).getConnection());
                }
            }
        });
        nifPanel.add(interfaces);
        JScrollPane spNifs = new JScrollPane(nifPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        spNifs.setViewportBorder(BorderFactory.createEmptyBorder());
        JPanel snmpPanel = new JPanel(new BorderLayout());
        snmpPanel.setBorder(BorderFactory.createTitledBorder("SNMP:"));
        JPanel inner = new JPanel(new BorderLayout());
        snmpLeft = new JList<>();
        snmpRight = new JList<>();
        snmpLeft.addListSelectionListener(e -> snmpRight.setSelectedIndex(snmpLeft.getSelectedIndex()));
        snmpRight.addListSelectionListener(e -> snmpLeft.setSelectedIndex(snmpRight.getSelectedIndex()));
        inner.add(snmpLeft, BorderLayout.WEST);
        inner.add(snmpRight, BorderLayout.CENTER);
        JScrollPane spSnmp = new JScrollPane(inner, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        spSnmp.setViewportBorder(BorderFactory.createEmptyBorder());
        snmpPanel.add(spSnmp, BorderLayout.CENTER);
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("device.history")));
        history = new JTextArea();
        history.setEditable(false);
        history.setTabSize(2);
        history.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
        JScrollPane spHistory = new JScrollPane(history, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        spHistory.setViewportBorder(BorderFactory.createEmptyBorder());
        historyPanel.add(spHistory, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(0, 2, 5, 5));
        buttons.setMaximumSize(new Dimension(2000, 30));
        prefs = new JButton(new AbstractAction(Lang.get("device.properties"), Icons.get("properties")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (current != null)
                    new DeviceProperties(owner, current, false);
            }
        });
        prefs.setEnabled(false);
        ports = new JButton(new AbstractAction(Lang.getNoHTML("port.scan"), Icons.get("find")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (current != null && current.getInterfaces().size() > 0) {
                    try {
                        new PortScanner(owner, current.getInterfaces().get(0).
                                getAddress().getHostAddress());
                    } catch (Exception ex) {/*meh*/}
                }
            }
        });
        ports.setEnabled(false);
        buttons.add(prefs);
        buttons.add(ports);
        add(Box.createVerticalStrut(5));
        add(infoPanel);
        add(spNifs);
        add(historyPanel);
        add(snmpPanel);
        add(buttons);
    }

    @Override
    public void graphReleased(Device device, MouseEvent event) {
    }

    @Override
    public void graphPressed(Device device, MouseEvent event) {
    }

    @Override
    public void graphClicked(final Device device, MouseEvent event) {
        prefs.setEnabled(true);
        ports.setEnabled(true);
        current = device;
        updateDeviceLabel(device, icon);
        description.setText(device.getDesctription());
        DateFormat df = new SimpleDateFormat(Settings.get("view.dateformat", "yyyy-MM-dd HH:mm"));
        StringBuilder sbStat = new StringBuilder();
        sbStat.append("<html>").append("<span style=\"color: ").append(device.getStatus().getHtmlValue()).append(";\">");
        sbStat.append(device.getStatus().getMessage()).append("</span>");
        if (device.getLastSeen() != null) {
            sbStat.append(" ").append(Lang.getNoHTML("since")).append(": ").append(df.format(device.getLastSeen()));
        }
        status.setText(sbStat.append("</html>").toString());
        Vector<Object> interfacesAndPorts = new Vector<>(device.getNrOfPorts());
        interfacesAndPorts.addAll(device.getInterfaces());
        for (int i = device.getInterfaces().size(); i < device.getNrOfPorts(); i++) {
            interfacesAndPorts.add(Lang.getNoHTML("device.port") + " " + (i + 1));
        }
        interfaces.setListData(interfacesAndPorts);
        Scheduler.execute(() -> snmpScan(device));
        StringBuilder sbHist = new StringBuilder();
        Status prevStatus = Status.UNKNOWN;
        for (Tuple<Date, Status> tuple : device.getStatusHistory()) {
            if (prevStatus != tuple.getSecond() && tuple.getSecond() != Status.UNKNOWN) {
                sbHist.insert(0, df.format(tuple.getFirst())+"  \t"+tuple.getSecond().getMessage()+"\n");
                prevStatus = tuple.getSecond();
            }
        }
        history.setText(sbHist.toString());
    }

    public static void updateDeviceLabel(Device device, JLabel label) {
        label.setIcon((Host.otherType.equals(device.getType())) ? Icons.fromBase64(device.getOtherID()) : Icons.getCisco(device.getType().toLowerCase()));
        StringBuilder sbProps = new StringBuilder();
        sbProps.append("<html>").append(device.getName());
        if (!device.getVendor().isEmpty()) sbProps.append("<br>").append(device.getVendor());
        if (!device.getModel().isEmpty()) sbProps.append(", ").append(device.getModel());
        if (!device.getLocation().isEmpty()) sbProps.append("<br>").append(device.getLocation());
        sbProps.append("</html>");
        label.setText(sbProps.toString());
    }

    private void snmpScan(Device d) {
        if (d == null) return;
        InetAddress address = null;
        for (NetworkIF nif : d.getInterfaces()) {
            if (PortScan.isOpen(nif.getAddress(), 161)) {
                address = nif.getAddress();
                break;
            }
        }
        Map<String, String> map = SNMP.getValues(address);
        if (map == null) {
            snmpLeft.setListData(new String[]{""});
            snmpRight.setListData(new String[]{""});
            return;
        }
        Vector<String> left = new Vector<>(), right = new Vector<>();
        for (String s : new TreeSet<>(map.keySet())) {
            left.add(s);
            right.add(map.get(s));
        }
        if (d != current) return; //abort is selection has changes
        snmpLeft.setListData(left);
        snmpRight.setListData(right);
    }

    private class IFListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (!(value instanceof NetworkIF)) {
                label.setText("<html><span style=\"color: #999999;\">" + label.getText() + "</span></html>");
                return label;
            }
            NetworkIF nif = (NetworkIF) value;
            StringBuilder sb = new StringBuilder();
            sb.append("<span style=\"color: ").append(nif.getStatus().getHtmlValue()).append(";\">&#8226; </span>");
            sb.append(nif.getName());
            if (nif.getAddress() != null) {
                sb.append(": ").append(nif.getAddress().getHostAddress());
            } else if (nif instanceof TransparentIF) {
                InetAddress addr = ((TransparentIF) nif).getCounterpart().getAddress();
                if (addr != null) sb.append(" &rarr; ").append(addr.getHostAddress());
            }
            if (Status.UP.equals(nif.getStatus())) sb.append(" (").append(nif.getLatency()).append("ms)");
            if (nif instanceof PhysicalIF) {
                PhysicalIF pif = (PhysicalIF) nif;
                if (pif.getMacAddress() != null && !pif.getMacAddress().isEmpty()) {
                    sb.append("<br>&nbsp;&nbsp;&nbsp;").append(pif.getMacAddress());
                    String oui = OUI.getInstance().lookup(pif.getMacAddress());
                    if (!"".equals(oui)) sb.append(" - <small>").append(oui).append("</small>");
                }
            }
            label.setText("<html>" + sb.toString() + "</html>");
            return label;
        }
    }
}
