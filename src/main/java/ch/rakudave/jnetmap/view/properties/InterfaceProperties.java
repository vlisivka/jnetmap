package ch.rakudave.jnetmap.view.properties;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.controller.command.Command;
import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.IF.PhysicalIF;
import ch.rakudave.jnetmap.model.IF.TransparentIF;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.net.OUI;
import ch.rakudave.jnetmap.net.Subnet;
import ch.rakudave.jnetmap.net.status.*;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;
import ch.rakudave.jnetmap.view.components.EscapableDialog;
import ch.rakudave.jnetmap.view.components.PortScanner;
import ch.rakudave.jnetmap.view.components.TabPanel;
import ch.rakudave.jnetmap.view.preferences.PreferencePanel;

import javax.swing.*;
import javax.swing.JSpinner.NumberEditor;
import java.awt.*;
import java.awt.event.*;
import java.util.Collections;
import java.util.Vector;
import java.util.function.Consumer;

@SuppressWarnings("serial")
public class InterfaceProperties extends EscapableDialog {
    public InterfaceProperties(final Frame owner, final NetworkIF i) {
        this(owner, i, null);
    }

    public InterfaceProperties(final Frame owner, final NetworkIF i, Consumer<Boolean> wasSaved) {
        super(owner, Lang.getNoHTML("interface.properties"));
        setLayout(new BorderLayout(5, 5));
        setPreferredSize(new Dimension(350, 420));
        setMinimumSize(new Dimension(350, 420));
        final PreferencePanel main = getInnerPanel(owner, i, true);
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 5));
        final JDialog _this = this;
        JButton cancel = new JButton(Lang.get("action.cancel"), Icons.get("cancel"));
        cancel.addActionListener(e -> {
            if (wasSaved != null) wasSaved.accept(false);
            _this.dispose();
        });
        JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
        ok.setPreferredSize(cancel.getPreferredSize());
        ok.addActionListener(e -> {
            main.save();
            if (wasSaved != null) wasSaved.accept(true);
            _this.dispose();
            if (TabPanel.getCurrentTab() != null) TabPanel.getCurrentTab().repaint();
        });
        bottomRow.add(cancel);
        bottomRow.add(ok);
        add(main, BorderLayout.CENTER);
        add(bottomRow, BorderLayout.SOUTH);
        pack();
        SwingHelper.centerTo(owner, this);
        setVisible(true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static PreferencePanel getInnerPanel(final Frame owner, final NetworkIF i, boolean showCounterpart) {
        if (i == null) return null;
        final boolean isPhysical = (i instanceof PhysicalIF), isTransparent = (i instanceof TransparentIF);
        final String oldAddress = (!isTransparent && i != null && i.getAddress() != null) ? i.getAddress().getHostAddress() : "127.0.0.1",
                oldSubnet = (i == null || i.getSubnet() == null) ? "" : i.getSubnet().getInfo().getNetmask(),
                oldGateway = (i == null || i.getGateway() == null) ? "" : i.getGateway().getHostAddress();
        final JTextField name = new JTextField(i.getName());
        final JTextField address = new JTextField((!isTransparent) ? oldAddress : "");
        address.setEnabled(!isTransparent);
        try {
            address.setToolTipText(i.getAddress().getCanonicalHostName());
        } catch (Exception e) {}
        final JTextField subnet = new JTextField(oldSubnet);
        subnet.setEnabled(!isTransparent);
        final JTextField gateway = new JTextField();
        gateway.setEnabled(isPhysical);
        address.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                autocomplete(address, subnet, gateway, false);
            }

            @Override
            public void focusGained(FocusEvent e) {
            }
        });
        address.addActionListener(e -> autocomplete(address, subnet, gateway, true));
        final JLabel oui = new JLabel("");
        final JTextField mac = new JTextField();
        mac.setEnabled(isPhysical);
        mac.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                oui.setText("<html><small>" + OUI.getInstance().lookup(mac.getText()) + "</small></html>");
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }
        });
        final JPanel portWrapper = new JPanel(new BorderLayout(5, 5));
        final PingMethod oldMethod = (isPhysical) ? ((PhysicalIF) i).getPingMethod() : null;
        int portNr = (isPhysical && oldMethod instanceof OpenSocket) ? ((OpenSocket) ((PhysicalIF) i).getPingMethod()).getPort() : 0;
        final JSpinner port = new JSpinner(new SpinnerNumberModel(portNr, 0, 65535, 1));
        port.setEditor(new NumberEditor(port, "#####"));
        port.setEnabled(isPhysical);
        final JButton portScan = new JButton(new AbstractAction("", Icons.get("find")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                PortScanner ps = new PortScanner(owner, i.getAddress().getHostAddress());
                port.setValue(ps.getPort());
            }
        });
        portScan.setEnabled(isPhysical);
        portWrapper.add(port, BorderLayout.CENTER);
        portWrapper.add(portScan, BorderLayout.EAST);
        final JComboBox method = new JComboBox(new String[]{"Java Ping", "System Ping", "TCP Port", "Nmap Ping", "Dummy Ping"});
        method.addActionListener(e -> {
            boolean en = method.getSelectedIndex() == 2;
            port.setEnabled(en);
            portScan.setEnabled(en);
        });
        method.setEnabled(isPhysical);
        if (oldMethod != null) method.setSelectedItem(oldMethod.toString());
        if (isPhysical) {
            gateway.setText(oldGateway);
            String macAddress = ((PhysicalIF) i).getMacAddress();
            mac.setText(macAddress);
            oui.setText("<html><small>" + OUI.getInstance().lookup(macAddress) + "</small></html>");
        }
        final JCheckBox intIgnore = new JCheckBox(Lang.getNoHTML("event.ignore.text"));
        if (isPhysical) intIgnore.setSelected(((PhysicalIF) i).isIgnore());
        intIgnore.setEnabled(isPhysical);
        intIgnore.setToolTipText(Lang.getNoHTML("event.ignore.text"));
        final JCheckBox addressCheckbox = new JCheckBox(Lang.get("interface.address"));
        addressCheckbox.setEnabled(isPhysical || isTransparent);
        addressCheckbox.setSelected(isPhysical);
        addressCheckbox.addActionListener(e -> {
            boolean s = addressCheckbox.isSelected();
            address.setEnabled(s);
            subnet.setEnabled(s);
            gateway.setEnabled(s);
            mac.setEnabled(s);
            method.setEnabled(s);
            port.setEnabled(s);
            portScan.setEnabled(s);
            intIgnore.setEnabled(s);
        });
        final JComboBox<Device> counterpart = new JComboBox<>();
        if (showCounterpart) {
            Vector devices = new Vector<>(Controller.getCurrentMap().getVertices());
            Collections.sort(devices);
            counterpart.setModel(new DefaultComboBoxModel<Device>(devices));
            counterpart.setSelectedItem(Controller.getCurrentMap().getOpposite(i.getDevice(), i.getConnection()));
        }
        PreferencePanel p = new PreferencePanel() {
            @Override
            public void save() {
                //TODO skip new command if nothing has changed
                Controller.getCurrentMap().getHistory().execute(new Command() {
                    String oldName = i.getName(), newName = name.getText(),
                            newAddress = address.getText(),
                            newSubnet = subnet.getText(), newGateway = gateway.getText(),
                            oldMac = (isPhysical) ? ((PhysicalIF) i).getMacAddress() : null, newMac = mac.getText();
                    PingMethod newMethod = nameToMethod((String) method.getSelectedItem(), (Integer) port.getValue());
                    boolean oldIgnore = isPhysical && ((PhysicalIF) i).isIgnore(), newIgnore = intIgnore.isSelected();
                    Connection connection = i.getConnection();
                    Device parent = i.getDevice();
                    Device oldCounterpart = Controller.getCurrentMap().getOpposite(parent, connection);
                    Device newCounterpart = (Device) counterpart.getSelectedItem();
                    NetworkIF newIF = null;

                    @Override
                    public Object undo() {
                        if (isPhysical && !addressCheckbox.isSelected()) {
                            PhysicalIF pif = new PhysicalIF(parent, connection, oldAddress);
                            pif.setSubnet(oldSubnet);
                            pif.setGateway(oldGateway);
                            pif.setMacAddress(oldMac);
                            pif.setPingMethod(oldMethod);
                            pif.setName(oldName);
                            pif.setIgnore(oldIgnore);
                            parent.addInterface(pif);
                            parent.removeInterface(newIF);
                        } else if (isTransparent && addressCheckbox.isSelected()) {
                            TransparentIF tif = new TransparentIF(parent, connection, Controller.getCurrentMap().
                                    getOpposite(parent, i.getConnection()).getInterfaceFor(connection));
                            tif.setName(oldName);
                            parent.addInterface(tif);
                            parent.removeInterface(newIF);
                        } else {
                            i.setAddress(oldAddress);
                            i.setSubnet(oldSubnet);
                            i.setGateway(oldGateway);
                            i.setName(oldName);
                            if (isPhysical) {
                                PhysicalIF pif = ((PhysicalIF) i);
                                pif.setMacAddress(oldMac);
                                pif.setPingMethod(oldMethod);
                                pif.setIgnore(oldIgnore);
                            }
                        }
                        if (oldCounterpart != null && newCounterpart != null && oldCounterpart != newCounterpart) {
                            Controller.getCurrentMap().removeEdge(connection);
                            Controller.getCurrentMap().addEdge(connection, parent, oldCounterpart);
                            connection.clearStatusMap();
                        }
                        return null;
                    }

                    @Override
                    public Object redo() {
                        NetworkIF counterpart = Controller.getCurrentMap().
                                getOpposite(parent, i.getConnection()).getInterfaceFor(connection);
                        if (isTransparent && addressCheckbox.isSelected()) {
                            PhysicalIF pif = new PhysicalIF(parent, connection, newAddress);
                            pif.setSubnet(newSubnet);
                            pif.setGateway(newGateway);
                            pif.setMacAddress(newMac);
                            pif.setPingMethod(newMethod);
                            pif.setName(newName);
                            pif.setIgnore(newIgnore);
                            newIF = pif;
                            parent.addInterface(pif);
                            parent.removeInterface(i);
                            if (counterpart instanceof TransparentIF) ((TransparentIF) counterpart).setCounterpart(pif);
                        } else if (isPhysical && !addressCheckbox.isSelected()) {
                            newIF = new TransparentIF(parent, connection, counterpart);
                            newIF.setName(newName);
                            parent.addInterface(newIF);
                            parent.removeInterface(i);
                        } else {
                            i.setAddress(newAddress);
                            i.setSubnet(newSubnet);
                            i.setGateway(newGateway);
                            i.setName(newName);
                            if (isPhysical) {
                                PhysicalIF pif = ((PhysicalIF) i);
                                pif.setMacAddress(newMac);
                                pif.setPingMethod(newMethod);
                                pif.setIgnore(newIgnore);
                            }
                        }
                        if (oldCounterpart != null && newCounterpart != null && oldCounterpart != newCounterpart) {
                            Controller.getCurrentMap().removeEdge(connection);
                            Controller.getCurrentMap().addEdge(connection, parent, newCounterpart);
                            connection.clearStatusMap();
                        }
                        return null;
                    }

                    @Override
                    public String toString() {
                        return Lang.getNoHTML("command.update.interface")+": "+i.getName()+" ("+parent.getName()+")";
                    }
                });
            }
        };
        p.setLayout(new GridLayout(0, 2, 5, 5));
        p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        p.add(new JLabel(Lang.get("interface.name")));
        p.add(name);
        p.add(addressCheckbox);
        p.add(address);
        p.add(new JLabel("      " + Lang.getNoHTML("interface.subnet")));
        p.add(subnet);
        p.add(new JLabel("      " + Lang.getNoHTML("interface.gateway")));
        p.add(gateway);
        p.add(new JLabel("      " + Lang.getNoHTML("interface.mac")));
        p.add(mac);
        p.add(new JLabel(""));
        p.add(oui);
        p.add(new JLabel("      " + Lang.getNoHTML("interface.pingmethod")));
        p.add(method);
        p.add(new JLabel());
        p.add(portWrapper);
        p.add(new JLabel("      " + Lang.getNoHTML("event.ignore.title")));
        p.add(intIgnore);
        if (showCounterpart) {
            p.add(new JLabel("      " + Lang.getNoHTML("interface.connectedto")));
            p.add(counterpart);
        }
        if (isPhysical) address.requestFocus();
        else name.requestFocus();
        return p;
    }

    // TODO do this with a list renderer instead of converting from String and back
    private static PingMethod nameToMethod(String name, int port) {
        if ("Java Ping".equals(name)) {
            return IsReachable.getInstance();
        } else if ("System Ping".equals(name)) {
            return NativePing.getInstance();
        } else if ("Nmap Ping".equals(name)) {
            return NmapPing.getInstance();
        } else if ("Dummy Ping".equals(name)) {
            return new DummyPing();
        } else {
            return new OpenSocket(port);
        }
    }

    private static void autocomplete(JTextField address, JTextField subnet, JTextField gateway, boolean force) {
        try {
            // attempt auto-completion
            String addr = address.getText(), mask = subnet.getText();
            mask = (mask.isEmpty()) ? "255.255.255.0" : mask;
            Subnet s;
            if (addr.contains("/")) s = new Subnet(addr);
            else s = new Subnet(addr, mask);
            address.setText(s.getInfo().getAddress());
            if (subnet.getText().isEmpty() || force) subnet.setText(s.getInfo().getNetmask());
            if (gateway.getText().isEmpty() || force) gateway.setText(s.getInfo().getLowAddress());
        } catch (Exception ex) {/*just trying, no harm done*/}
    }
}
