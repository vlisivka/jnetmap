package ch.rakudave.jnetmap.view.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

@SuppressWarnings("serial")
public class EscapableDialog extends JDialog {
    public EscapableDialog(Window owner, String title) {
        super(owner, title, ModalityType.MODELESS);
        getRootPane().registerKeyboardAction(actionEvent -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}
