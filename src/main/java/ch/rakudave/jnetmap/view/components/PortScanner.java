package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.net.PortScan;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;
import ch.rakudave.jnetmap.util.logging.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.InetAddress;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

@SuppressWarnings("serial")
public class PortScanner extends EscapableDialog {
    private int port;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public PortScanner(final Frame owner, String address) {
        super(owner, Lang.getNoHTML("port.scanner"));
        try {
            final PortScanner _this = this;
            setLayout(new BorderLayout(5, 5));
            setPreferredSize(new Dimension(500, 500));
            setMinimumSize(new Dimension(500, 500));
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JPanel main = new JPanel(new BorderLayout(5, 5));
            JPanel listHolder = new JPanel(new BorderLayout()); // I hate JTables. With a passion.
            final JList leftList = new JList();
            leftList.setPreferredSize(new Dimension(55, 400));
            final JList rightList = new JList();
            leftList.addListSelectionListener(e -> {
                rightList.setSelectedIndex(leftList.getSelectedIndex());
                try {
                    if (_this != null) _this.port = Integer.valueOf((String) leftList.getSelectedValue());
                } catch (Exception ex) {
                    Logger.debug("Unable to get int-value from list", ex);
                    _this.port = 0;
                }
            });
            rightList.addListSelectionListener(e -> leftList.setSelectedIndex(rightList.getSelectedIndex()));
            listHolder.add(leftList, BorderLayout.WEST);
            listHolder.add(rightList, BorderLayout.CENTER);
            JPanel topPanel = new JPanel(new BorderLayout());
            JPanel topInner = new JPanel(new GridLayout(0, 1, 5, 5));
            final JTextField addr = new JTextField(address);
            final JComboBox scanType = new JComboBox(new String[]{Lang.get("port.wellknown"), Lang.get("port.registered"), Lang.get("port.all"), Lang.get("port.manual")});
            final JPanel manualPorts = new JPanel(new GridLayout(1, 0, 5, 5));
            final JSpinner fromPort = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
            fromPort.setEnabled(false);
            fromPort.setEditor(new JSpinner.NumberEditor(fromPort, "#"));
            final JSpinner toPort = new JSpinner(new SpinnerNumberModel(1023, 0, 65535, 1));
            toPort.setEnabled(false);
            toPort.setEditor(new JSpinner.NumberEditor(toPort, "#"));
            manualPorts.add(fromPort);
            manualPorts.add(toPort);
            scanType.addActionListener(e -> {
                fromPort.setEnabled(scanType.getSelectedIndex() == 3);
                toPort.setEnabled(scanType.getSelectedIndex() == 3);
            });
            topInner.add(addr);
            topInner.add(scanType);
            topInner.add(manualPorts);
            JButton scan = new JButton(new AbstractAction(Lang.get("port.scan")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        InetAddress inetAddr;
                        try {
                            inetAddr = InetAddress.getByName(addr.getText());
                        } catch (Exception e1) {
                            Logger.debug("Invalid address", e1);
                            JOptionPane.showMessageDialog(null, Lang.get("message.address.invalid"), Lang.getNoHTML("message.error"), JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        Map<Integer, String> map;
                        if (scanType.getSelectedIndex() == 0) {
                            map = PortScan.wellKnownPortsScan(inetAddr);
                        } else if (scanType.getSelectedIndex() == 1) {
                            map = PortScan.registeredPortscan(inetAddr);
                        } else if (scanType.getSelectedIndex() == 1) {
                            map = PortScan.allPortsScan(inetAddr);
                        } else {
                            map = PortScan.scan(inetAddr, Integer.valueOf(fromPort.getValue().toString()), Integer.valueOf(toPort.getValue().toString()));
                        }
                        Vector<String> leftData = new Vector<>();
                        Vector<String> rightData = new Vector<>();
                        for (int i : new TreeSet<>(map.keySet())) {
                            leftData.add(String.valueOf(i));
                            rightData.add(map.get(i));
                        }
                        leftList.setListData(leftData);
                        rightList.setListData(rightData);
                    } catch (Throwable e2) {
                        Logger.debug("Port scanner failed", e2);
                    }
                }
            });
            scan.setPreferredSize(new Dimension(70, 100));
            topPanel.add(topInner, BorderLayout.CENTER);
            topPanel.add(scan, BorderLayout.EAST);
            main.add(topPanel, BorderLayout.NORTH);
            main.add(listHolder);
            JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 5));
            JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
            ok.setPreferredSize(new Dimension(60, 30));
            ok.addActionListener(e -> _this.dispose());
            bottomRow.add(ok);
            add(main, BorderLayout.CENTER);
            add(bottomRow, BorderLayout.SOUTH);
            pack();
            SwingHelper.centerTo(owner, this);
            setVisible(true);
        } catch (Throwable t) {
            Logger.error("Port scanner fail", t);
            dispose();
        }
    }

    public int getPort() {
        return port;
    }
}
