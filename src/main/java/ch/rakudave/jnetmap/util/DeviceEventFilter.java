package ch.rakudave.jnetmap.util;

import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.DeviceEvent;
import ch.rakudave.jnetmap.model.device.DeviceEvent.Type;
import ch.rakudave.jnetmap.net.status.Status;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class DeviceEventFilter {
    private boolean onDeviceChange = true, onIFChange;
    private boolean[] status = {true, true, true, true};
    private String deviceNameMatcher = "", statusMatcher = "", ipMatcher = "", subnetMatcher = "";
    private int quietFrom, quietTo;

    public DeviceEventFilter() {
    }

    public DeviceEventFilter(boolean onDeviceChange, boolean onIFChange) {
        this.onDeviceChange = onDeviceChange;
        this.onIFChange = onIFChange;
    }

    public boolean matches(DeviceEvent e) {
        legacyFilterRestoration();
        if (!onDeviceChange && !onIFChange) return false;
        Device d = (Device) e.getSource();
        if (!deviceNameMatcher.isEmpty() && !d.getName().matches(deviceNameMatcher)) return false;
        if (onDeviceChange && Type.STATUS_CHANGED.equals(e.getType())) {
            if ((Status.UP.equals(d.getStatus()) && !status[0]) || (Status.DOWN.equals(d.getStatus()) && !status[1]) ||
                    (Status.NOT_FOUND.equals(d.getStatus()) && !status[2]) || (Status.UNKNOWN.equals(d.getStatus()) && !status[3]))
                return false;
            for (NetworkIF nif : d.getInterfaces()) {
                if (!ipMatcher.isEmpty() && nif.getAddress() != null
                        && !nif.getAddress().getHostAddress().matches(ipMatcher)) return false;
                if (!subnetMatcher.isEmpty() && nif.getSubnet() != null
                        && !nif.getSubnet().getInfo().getBroadcastAddress().matches(ipMatcher)) return false;
            }
            return isWithinTimeLimit(e);
        } else if (onIFChange && Type.INTERFACE_STATUS_CHANGED.equals(e.getType())) {
            NetworkIF nif = (NetworkIF) e.getSubject();
            if ((Status.UP.equals(d.getStatus()) && !status[0]) || (Status.DOWN.equals(d.getStatus()) && !status[1]) ||
                    (Status.NOT_FOUND.equals(d.getStatus()) && !status[2]) || (Status.UNKNOWN.equals(d.getStatus()) && !status[3]))
                return false;
            if (!ipMatcher.isEmpty() && nif.getAddress() != null
                    && !nif.getAddress().getHostAddress().matches(ipMatcher)) return false;
            return !(!subnetMatcher.isEmpty() && nif.getSubnet() != null
                    && !nif.getSubnet().getInfo().getBroadcastAddress().matches(ipMatcher)) && statusMatcher.isEmpty() && ipMatcher.isEmpty() && subnetMatcher.isEmpty() && isWithinTimeLimit(e);
        } else {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isWithinTimeLimit(DeviceEvent e) {
        if (quietFrom == quietTo) return true;
        int now = e.getTime().getHours() * 60 + e.getTime().getMinutes();
        if (quietFrom < quietTo) {
            return (now < quietFrom) || (now > quietTo);
        } else {
            return (now < quietFrom) && (now > quietTo);
        }
    }

    public JPanel settingsPanel() {
        legacyFilterRestoration();
        JPanel p = new JPanel(new GridLayout(0, 2, 5, 5));
        p.setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("filter")));
        final JCheckBox deviceChange = new JCheckBox(Lang.get("device"), onDeviceChange);
        final JCheckBox ifChange = new JCheckBox(Lang.get("interface"), onIFChange);
        JPanel statusFilter1 = new JPanel(new GridLayout(1, 2));
        final JCheckBox up = new JCheckBox(Status.UP.getMessage(), status[0]);
        final JCheckBox down = new JCheckBox(Status.DOWN.getMessage(), status[1]);
        statusFilter1.add(up);
        statusFilter1.add(down);
        JPanel statusFilter2 = new JPanel(new GridLayout(1, 2));
        final JCheckBox not_found = new JCheckBox(Status.NOT_FOUND.getMessage(), status[2]);
        final JCheckBox unknown = new JCheckBox(Status.UNKNOWN.getMessage(), status[3]);
        statusFilter2.add(not_found);
        statusFilter2.add(unknown);
        final JTextField deviceFilter = new JTextField(deviceNameMatcher);
        deviceFilter.setToolTipText("Regular Expression");
        final JTextField ipFilter = new JTextField(ipMatcher);
        ipFilter.setToolTipText("Regular Expression");
        final JTextField subFilter = new JTextField(subnetMatcher);
        subFilter.setToolTipText("Regular Expression");
        FocusListener f = new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                onDeviceChange = deviceChange.isSelected();
                onIFChange = ifChange.isSelected();
                deviceNameMatcher = deviceFilter.getText();
                ipMatcher = ipFilter.getText();
                subnetMatcher = subFilter.getText();
            }
        };
        deviceChange.addFocusListener(f);
        ifChange.addFocusListener(f);
        deviceFilter.addFocusListener(f);
        ipFilter.addFocusListener(f);
        subFilter.addFocusListener(f);
        ActionListener al = arg0 -> {
            status[0] = up.isSelected();
            status[1] = down.isSelected();
            status[2] = not_found.isSelected();
            status[3] = unknown.isSelected();
        };
        up.addActionListener(al);
        down.addActionListener(al);
        not_found.addActionListener(al);
        unknown.addActionListener(al);
        JPanel quietTime = new JPanel(new GridLayout(1, 5, 5, 5));
        final SpinnerNumberModel quietFromHour = new SpinnerNumberModel(quietFrom / 60, 0, 23, 1);
        final SpinnerNumberModel quietFromMin = new SpinnerNumberModel(quietFrom % 60, 0, 59, 1);
        ChangeListener cl = e -> quietFrom = quietFromHour.getNumber().intValue() * 60 + quietFromMin.getNumber().intValue();
        quietFromHour.addChangeListener(cl);
        quietFromMin.addChangeListener(cl);
        final SpinnerNumberModel quietToHour = new SpinnerNumberModel(quietTo / 60, 0, 23, 1);
        final SpinnerNumberModel quietToMin = new SpinnerNumberModel(quietTo % 60, 0, 59, 1);
        cl = e -> quietTo = quietToHour.getNumber().intValue() * 60 + quietToMin.getNumber().intValue();
        quietToHour.addChangeListener(cl);
        quietToMin.addChangeListener(cl);
        quietTime.add(new JSpinner(quietFromHour));
        quietTime.add(new JSpinner(quietFromMin));
        quietTime.add(new JLabel("-", JLabel.CENTER));
        quietTime.add(new JSpinner(quietToHour));
        quietTime.add(new JSpinner(quietToMin));
        p.add(new JLabel(Lang.get("event.from")));
        p.add(deviceChange);
        p.add(new JLabel(Lang.get("event.from")));
        p.add(ifChange);
        p.add(new JLabel(Lang.get("filter.status")));
        p.add(statusFilter1);
        p.add(new JLabel());
        p.add(statusFilter2);
        p.add(new JLabel(Lang.get("filter.name")));
        p.add(deviceFilter);
        p.add(new JLabel(Lang.get("filter.ip")));
        p.add(ipFilter);
        p.add(new JLabel(Lang.get("filter.subnet")));
        p.add(subFilter);
        p.add(new JLabel(Lang.get("event.quiet")));
        p.add(quietTime);
        return p;
    }

    private void legacyFilterRestoration() {
        if (status == null) status = new boolean[]{true, true, true, true};
        if (statusMatcher == null) statusMatcher = "";
        if (!statusMatcher.isEmpty()) {
            status[0] = Status.UP.getMessage().matches(statusMatcher);
            status[1] = Status.DOWN.getMessage().matches(statusMatcher);
            status[2] = Status.NOT_FOUND.getMessage().matches(statusMatcher);
            status[3] = Status.UNKNOWN.getMessage().matches(statusMatcher);
            statusMatcher = "";
        }
    }

}
