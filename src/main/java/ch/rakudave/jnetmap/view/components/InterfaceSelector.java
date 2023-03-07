package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

@SuppressWarnings("serial")
public class InterfaceSelector extends JDialog {
    private String address;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public InterfaceSelector(Frame owner, Device d, Vector<String> addresses) {
        super(owner, d.getName(), ModalityType.DOCUMENT_MODAL);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 115));
        final JComboBox nifs = new JComboBox(addresses);
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 5));
        final JDialog _this = this;
        JButton cancel = new JButton(Lang.get("action.cancel"), Icons.get("cancel"));
        cancel.addActionListener(e -> _this.dispose());
        JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
        ok.setPreferredSize(cancel.getPreferredSize());
        ok.addActionListener(e -> {
            address = (String) nifs.getSelectedItem();
            _this.dispose();
        });
        bottomRow.add(cancel);
        bottomRow.add(ok);
        add(new JLabel(Lang.get("message.select.interface")), BorderLayout.NORTH);
        add(nifs, BorderLayout.CENTER);
        add(bottomRow, BorderLayout.SOUTH);
        pack();
        SwingHelper.centerTo(owner, this);
        setVisible(true);
    }

    public String getSelected() {
        return address;
    }
}