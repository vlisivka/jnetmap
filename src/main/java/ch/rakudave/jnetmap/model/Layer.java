package ch.rakudave.jnetmap.model;

import ch.rakudave.jnetmap.model.device.Device;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedHashSet;
import java.util.Set;

public class Layer {
    private String name;
    private boolean isVisible;
    private Set<Device> devices;
    @XStreamOmitField
    private JCheckBox checkBox;

    public Layer() {
        devices = new LinkedHashSet<>();
        isVisible = true;
        checkBox = new JCheckBox(name);
        checkBox.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mousePressed(MouseEvent e) {
                // TODO Auto-generated method stub
                setVisible(!isVisible);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // TODO Auto-generated method stub

            }
        });
    }

    public Layer(String name) {
        this();
        setName(name);
    }

    public JCheckBox getComponent() {
        return checkBox;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        checkBox.setText(name);
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
        checkBox.setSelected(isVisible);
    }

    public Set<Device> getDevices() {
        return devices;
    }

    public boolean containsDevice(Device device) {
        return devices.contains(device);
    }

    public void addDevice(Device device) {
        devices.add(device);
    }

    public void removeDevice(Device device) {
        devices.remove(device);
    }

    public void addDevices(Set<Device> devices) {
        this.devices.addAll(devices);
    }

}
