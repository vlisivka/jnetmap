package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.net.OUI;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

@SuppressWarnings("serial")
public class OUILookup extends EscapableDialog {

    public OUILookup(final Frame owner) {
        super(owner, Lang.getNoHTML("oui.lookup"));
        final OUILookup _this = this;
        setPreferredSize(new Dimension(280, 180));
        setMinimumSize(new Dimension(280, 180));
        JPanel container = new JPanel(new GridLayout(4, 1, 5, 5));
        container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        container.add(new JLabel(Lang.get("interface.mac")));//"<html>"+Lang.getNoHTML("interface.mac")+":</html>"));
        final JTextField oui = new JTextField("");
        oui.setEditable(false);
        final JTextField mac = new JTextField("");
        mac.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                oui.setEnabled(mac.getText().length() >= 8);
                oui.setText(OUI.getInstance().lookup(mac.getText()));
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) _this.dispose();
            }
        });
        container.add(mac);
        container.add(new JLabel(Lang.get("device.vendor")));
        container.add(oui);
        add(container);
        pack();
        SwingHelper.centerTo(owner, this);
        setVisible(true);
    }
}
