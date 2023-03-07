package ch.rakudave.jnetmap.view.properties;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.controller.command.Command;
import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.Host;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.components.EscapableDialog;
import ch.rakudave.jnetmap.view.components.TabPanel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class DeviceProperties extends EscapableDialog {
    private Device d;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DeviceProperties(final Frame owner, Device device, final boolean isNew) {
        super(owner, Lang.getNoHTML("device.properties"));
        d = device;
        setLayout(new BorderLayout(5, 5));
        setPreferredSize(new Dimension(400, 540));
        setMinimumSize(new Dimension(400, 500));
        final String oldType = (d.getType() != null) ? d.getType() : Host.fallbackType;
        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        final JLabel deviceIcon = new JLabel((Host.otherType.equals(oldType)) ? Icons.fromBase64(d.getOtherID()) : Icons.getCisco(oldType.toString().toLowerCase()));
        iconPanel.add(Box.createVerticalStrut(64));
        iconPanel.add(deviceIcon);
        iconPanel.add(Box.createVerticalStrut(64));
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        JPanel propPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        final JTextField otherID = new JTextField(d.getOtherID());
        final JComboBox typeCombo = new JComboBox(Host.defaultTypes);
        typeCombo.setSelectedItem(oldType);
        typeCombo.addActionListener(e -> {
            if ("other".equalsIgnoreCase(typeCombo.getSelectedItem().toString())) {
                File f = SwingHelper.openDialog(owner, new FileNameExtensionFilter("PNG Image", "png"));
                if (f != null) {
                    String base64 = Icons.getBase64(f);
                    otherID.setText(base64);
                    deviceIcon.setIcon(Icons.fromBase64(base64));
                } else {
                    deviceIcon.setIcon(Icons.getCisco(oldType.toString().toLowerCase()));
                    typeCombo.setSelectedItem(oldType);
                }
            } else {
                deviceIcon.setIcon(Icons.getCisco(typeCombo.getSelectedItem().toString().toLowerCase()));
                otherID.setText("");
            }
        });
        final JTextField deviceName = new JTextField(d.getName());
        final JTextField deviceDesc = new JTextField(d.getDesctription());
        final JTextField deviceLocation = new JTextField(d.getLocation());
        final JTextField deviceVendor = new JTextField(d.getVendor());
        final JTextField deviceModel = new JTextField(d.getModel());
        final JCheckBox deviceIgnore = new JCheckBox(Lang.get("event.ignore.text"));
        deviceIgnore.setSelected(d.isIgnore());
        propPanel.add(new JLabel(Lang.get("device.type")));
        propPanel.add(typeCombo);
        propPanel.add(new JLabel(Lang.get("device.name")));
        propPanel.add(deviceName);
        propPanel.add(new JLabel(Lang.get("device.description")));
        propPanel.add(deviceDesc);
        propPanel.add(new JLabel(Lang.get("device.location")));
        propPanel.add(deviceLocation);
        propPanel.add(new JLabel(Lang.get("device.vendor")));
        propPanel.add(deviceVendor);
        propPanel.add(new JLabel(Lang.get("device.model")));
        propPanel.add(deviceModel);
        propPanel.add(new JLabel(Lang.get("event.ignore.title")));
        propPanel.add(deviceIgnore);
        propPanel.add(new JLabel(Lang.get("device.interfaces")));
        propPanel.add(new JLabel());
        final SpinnerNumberModel nrOfPorts = new SpinnerNumberModel(d.getNrOfPorts(), d.getInterfaces().size(), 512, 1);
        IFListModel interfaceListModel = new IFListModel(d, nrOfPorts);
        final JList interfaceList = new JList(interfaceListModel);
        interfaceList.setCellRenderer(new IFListRenderer());
        interfaceList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                new InterfaceProperties(owner, ((NetworkIF) interfaceList.getSelectedValue()), saved -> {
                    if (saved) interfaceListModel.update();
                });
            }
            }
        });
        JPanel interfaceManipulators = new JPanel();
        interfaceManipulators.setLayout(new BoxLayout(interfaceManipulators, BoxLayout.PAGE_AXIS));
        interfaceManipulators.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        JButton removeInterface = new JButton(Lang.get("action.remove"), Icons.get("remove"));
        removeInterface.setPreferredSize(new Dimension(120, 30));
        removeInterface.addActionListener(e -> {
            if (!(interfaceList.getSelectedValue() instanceof NetworkIF)) return;
            if (JOptionPane.showConfirmDialog(owner, Lang.get("message.confirm.delete")
                            .replaceAll("%name%", ((NetworkIF) interfaceList.getSelectedValue()).getName()),
                    Lang.getNoHTML("action.delete"), JOptionPane.YES_NO_OPTION) == 1) return;
            Controller.getCurrentMap().removeEdge(((NetworkIF) interfaceList.getSelectedValue()).getConnection());
            interfaceListModel.update();
        });
        JButton editInterface = new JButton(Lang.get("action.edit"), Icons.get("edit"));
        editInterface.setPreferredSize(new Dimension(120, 30));
        editInterface.addActionListener(e -> {
            if (!(interfaceList.getSelectedValue() instanceof NetworkIF)) return;
            new InterfaceProperties(owner, ((NetworkIF) interfaceList.getSelectedValue()), saved -> {
                if (saved) interfaceListModel.update();
            });
        });
        final JSpinner nrOfPortsSpinner = new JSpinner(nrOfPorts);
        nrOfPortsSpinner.setMaximumSize(new Dimension(100, 30));
        nrOfPortsSpinner.addChangeListener(e -> interfaceListModel.update());
        JPanel intManWrapper = new JPanel(new GridLayout(4, 1));
        intManWrapper.add(editInterface);
        intManWrapper.add(removeInterface);
        intManWrapper.add(new JLabel(Lang.get("device.ports")));
        intManWrapper.add(nrOfPortsSpinner);
        interfaceManipulators.add(intManWrapper);
        interfaceManipulators.add(Box.createVerticalGlue());
        centerWrapper.add(propPanel, BorderLayout.NORTH);
        centerWrapper.add(new JScrollPane(interfaceList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        centerWrapper.add(interfaceManipulators, BorderLayout.EAST);
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        final JDialog _this = this;
        JButton cancel = new JButton(Lang.get("action.cancel"), Icons.get("cancel"));
        cancel.addActionListener(e -> _this.dispose());
        JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
        ok.setPreferredSize(cancel.getPreferredSize());
        ok.addActionListener(e -> {
            final String newType = (String) typeCombo.getSelectedItem();
            final String oldName = d.getName(), newName = deviceName.getText(),
                    oldID = d.getOtherID(), newID = otherID.getText(),
                    oldDesc = d.getDesctription(), newDesc = deviceDesc.getText(),
                    oldLocation = d.getLocation(), newLocation = deviceLocation.getText(),
                    oldVendor = d.getVendor(), newVentor = deviceVendor.getText(),
                    oldModel = d.getModel(), newModel = deviceModel.getText();
            final boolean oldIgnore = d.isIgnore(), newIgnore = deviceIgnore.isSelected();
            final int oldNrOfPorts = d.getNrOfPorts(), newNrOfPorts = (Integer) nrOfPorts.getValue();
            //TODO skip new command if nothing has changed
            Controller.getCurrentMap().getHistory().execute(new Command() {
                @Override
                public Object undo() {
                    d.setType(oldType);
                    d.setOtherID(oldID);
                    d.setName(oldName);
                    d.setDescription(oldDesc);
                    d.setLocation(oldLocation);
                    d.setVendor(oldVendor);
                    d.setModel(oldModel);
                    d.setIgnore(oldIgnore);
                    d.setNrOfPorts(oldNrOfPorts);
                    return null;
                }

                @Override
                public Object redo() {
                    d.setType(newType);
                    d.setOtherID(newID);
                    d.setName(newName);
                    d.setDescription(newDesc);
                    d.setLocation(newLocation);
                    d.setVendor(newVentor);
                    d.setModel(newModel);
                    d.setIgnore(newIgnore);
                    d.setNrOfPorts(newNrOfPorts);
                    return null;
                }

                @Override
                public String toString() {
                    return Lang.getNoHTML("command.update.device")+": "+d.getName();
                }
            });
            _this.dispose();
            if (TabPanel.getCurrentTab() != null) TabPanel.getCurrentTab().repaint();
        });
        if (!isNew) bottomRow.add(cancel);
        bottomRow.add(ok);
        add(iconPanel, BorderLayout.NORTH);
        add(centerWrapper, BorderLayout.CENTER);
        add(bottomRow, BorderLayout.SOUTH);
        pack();
        SwingHelper.centerTo(owner, this);
        deviceName.requestFocus();
        setVisible(true);
    }

    @SuppressWarnings({"rawtypes"})
    private class IFListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);
            if (!(value instanceof NetworkIF)) {
                label.setText("<html><span style=\"color: #999999;\">" + label.getText() + "</span></html>");
                return label;
            }
            NetworkIF nif = (NetworkIF) value;
            try {
                String color = nif.getStatus().getHtmlValue();
                label.setText("<html>" + nif + " <span style=\"color: " + color + ";\"> &rarr; </span>" +
                        Controller.getCurrentMap().getOpposite(d, nif.getConnection()) + "</html>");
            } catch (Exception e) {
                Logger.debug("Failed to get opposite of " + nif.toString(), e);
            }
            return label;
        }
    }

    private class IFListModel extends DefaultListModel<Object> {
        private Device device;
        private SpinnerNumberModel spinnerModel;

        public IFListModel(Device device, SpinnerNumberModel nrOfPorts) {
            this.device = device;
            spinnerModel = nrOfPorts;
        }

        @Override
        public int getSize() {
            return Math.max(device.getInterfaces().size(), (Integer) spinnerModel.getValue());
        }

        @Override
        public Object getElementAt(int index) {
            if (index < device.getInterfaces().size()) {
                return device.getInterfaces().get(index);
            } else {
                return Lang.getNoHTML("device.port") + " " + (index + 1);
            }
        }

        public void update() {
            fireContentsChanged(this, 0, getSize()-1);
        }
    }
}
