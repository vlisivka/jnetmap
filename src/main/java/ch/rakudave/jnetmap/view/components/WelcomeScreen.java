package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.controller.Actions;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;

import javax.swing.*;
import java.awt.*;

/**
 * This is the first thing a user will see when launching jNetMap.
 * It should greet him and offer a few things options to get him started.
 *
 * @author rakudave
 */
class WelcomeScreen extends JPanel {
    private static final long serialVersionUID = -6831398348123415315L;

    @SuppressWarnings("serial")
    public WelcomeScreen() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        final ImageIcon jNetMapLogo = new ImageIcon(getClass().getResource("/img/splash-tb.png"));
        JLabel logo = new JLabel(jNetMapLogo);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel buttonContainer = new JPanel(new GridLayout(0, 1, 0, 5)) {{
            setMaximumSize(new Dimension(jNetMapLogo.getIconWidth() + 20, 200));
            setAlignmentX(Component.CENTER_ALIGNMENT);
            add(createWelcomeButton(Actions.newMap(Lang.get("welcome.new"))));
            add(createWelcomeButton(Actions.open(Lang.get("welcome.open"))));
            add(createWelcomeButton(Actions.viewDoc(Lang.get("welcome.doc"))));
        }};
        JEditorPane warningText = SwingHelper.createHtmlLabel(Lang.get("welcome.warning"));
        warningText.setMaximumSize(new Dimension(jNetMapLogo.getIconWidth() + 16, 1000));
        add(logo);
        add(Box.createVerticalStrut(10));
        add(buttonContainer);
        add(Box.createVerticalStrut(10));
        add(warningText);
        add(Box.createVerticalGlue());
    }

    private JButton createWelcomeButton(Action a) {
        JButton b = new JButton(a);
        b.setHorizontalAlignment(JButton.LEADING);
        b.setFocusable(false);
        return b;
    }
}
