package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.controller.Scheduler;
import ch.rakudave.jnetmap.net.NetworkScan;
import ch.rakudave.jnetmap.net.Subnet;
import ch.rakudave.jnetmap.net.status.PingMethod;
import ch.rakudave.jnetmap.util.*;
import ch.rakudave.jnetmap.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@SuppressWarnings("serial")
public class NetworkScanner extends EscapableDialog {
    private NetworkScan netScan;
    private AlphanumComparator alphaComp = new AlphanumComparator();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public NetworkScanner(final Frame owner, final ch.rakudave.jnetmap.model.Map map) {
        super(owner, Lang.getNoHTML("network.scanner"));
        final NetworkScanner _this = this;
        setLayout(new BorderLayout(5, 5));
        setPreferredSize(new Dimension(500, 500));
        setMinimumSize(new Dimension(500, 500));
        JPanel main = new JPanel(new BorderLayout(5, 5));
        final JList list = new JList();
        list.setCellRenderer(new CheckListRenderer());
        list.addListSelectionListener(e -> {
            if (list.getSelectedValue() != null) {
                JCheckBox c = ((JCheckBox) list.getSelectedValue());
                c.setSelected(!c.isSelected());
            }
            list.clearSelection();
        });
        final Vector<JCheckBox> checkboxes = new Vector<>();
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel topInner = new JPanel(new GridLayout(2, 1, 5, 5));
        final JTextField addr = new JTextField(NetworkScan.getLocalAddress());
        final JTextField sub = new JTextField();
        addr.setToolTipText(Lang.get("network.scanner.address"));
        addr.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (sub.getText().isEmpty()) autoFill(addr, sub);
            }
        });
        addr.addActionListener(e -> autoFill(addr, sub));
        sub.setToolTipText(Lang.get("network.scanner.netmask"));
        autoFill(addr, sub);
        topInner.add(addr);
        topInner.add(sub);
        final JButton scan = new JButton(Lang.get("port.scan"));
        scan.addActionListener(e -> {
            try {
                if (netScan != null) return;
                scan.setEnabled(false);
                netScan = new NetworkScan(new Subnet(addr.getText(), sub.getText()), e12 -> {
                    InetAddress address = ((Tuple<InetAddress, PingMethod>) e12.getSource()).getFirst();
                    JCheckBox c = new JCheckBox(address.getHostAddress(), true);
                    c.setToolTipText(Lang.get("action.add"));
                    checkboxes.add(c);
                    if (NetworkScan.tryFindIP(map, address) != null) {
                        c.setEnabled(false);
                        c.setSelected(false);
                    }
                    Collections.sort(checkboxes, (o1, o2) -> alphaComp.compare(o1.getText(), o2.getText()));
                    list.setListData(checkboxes);
                });
                Scheduler.execute(() -> netScan.start(true));
                Scheduler.execute(() -> {
                    scan.setIcon(Icons.get("busy"));
                    scan.setText("");
                    while (!netScan.isDone()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e1) {
                            Logger.debug("Interrupted", e1);
                        }
                    }
                    scan.setIcon(null);
                    scan.setText(Lang.get("action.done"));
                });
            } catch (Exception ex) {
                Logger.error("Unable to conduct NetScan", ex);
            }
        });
        topPanel.add(topInner, BorderLayout.CENTER);
        topPanel.add(scan, BorderLayout.EAST);
        main.add(topPanel, BorderLayout.NORTH);
        main.add(new JScrollPane(list));
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 5));
        JButton cancel = new JButton(Lang.get("action.cancel"), Icons.get("cancel"));
        cancel.addActionListener(e -> {
            if (netScan != null) netScan.cancel();
            _this.dispose();
        });
        final JButton ok = new JButton(Lang.get("action.add"), Icons.get("add"));
        ok.addActionListener(e -> {
            if (netScan != null) {
                ok.setEnabled(false);
                if (!netScan.isDone()) netScan.cancel();
                Map<InetAddress, PingMethod> hosts = new HashMap<InetAddress, PingMethod>(netScan.getFoundHosts());
                for (JCheckBox c : checkboxes) {
                    if (!c.isSelected()) {
                        try {
                            hosts.remove(InetAddress.getByName(c.getText()));
                        } catch (UnknownHostException ex) {
                            Logger.warn("Failed to remove INetAddress ", ex);
                        }
                    }
                }
                Logger.debug("Adding to Map: " + Arrays.toString(hosts.keySet().toArray()));
                netScan.addToMap(hosts, map);
                _this.dispose();
            }
        });
        bottomRow.add(cancel);
        bottomRow.add(ok);
        add(main, BorderLayout.CENTER);
        add(bottomRow, BorderLayout.SOUTH);
        pack();
        SwingHelper.centerTo(owner, this);
        setVisible(true);
    }

    private void autoFill(JTextField address, JTextField subnet) {
        try {
            Subnet s = new Subnet((address.getText().contains("/")) ? address.getText() : address.getText() + "/24");
            address.setText(s.getInfo().getLowAddress());
            subnet.setText(s.getInfo().getNetmask());
        } catch (Exception e) {
            Logger.warn("Unable to autoFill address " + address.getText(), e);
        }
    }

    @SuppressWarnings({"rawtypes"})
    private class CheckListRenderer implements ListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            return (Component) value;
        }
    }

}
