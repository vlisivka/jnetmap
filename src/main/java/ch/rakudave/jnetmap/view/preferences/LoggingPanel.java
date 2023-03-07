package ch.rakudave.jnetmap.view.preferences;

import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Appender;
import ch.rakudave.jnetmap.util.logging.FileAppender;
import ch.rakudave.jnetmap.util.logging.ListenerAppender;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.util.logging.Logger.Level;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author rakudave
 */
@SuppressWarnings("serial")
public class LoggingPanel extends PreferencePanel {
    @SuppressWarnings("rawtypes")
    private HashMap<Appender, JComboBox> hm = new HashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public LoggingPanel() {
        title = Lang.getNoHTML("preferences.logging");
        // ComboBoxes
        JPanel comboPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        comboPanel.setMaximumSize(new Dimension(9999, 25));
        for (Appender a : Logger.getAppenders()) {
            if (!(a instanceof ListenerAppender)) {
                JComboBox cb = new JComboBox(Level.values());
                cb.setSelectedItem(a.getLevel());
                comboPanel.add(new JLabel(a.getClass().getSimpleName().replace("Appender", "")));
                comboPanel.add(cb);
                if (a instanceof FileAppender) {
                    comboPanel.add(new JLabel());
                    comboPanel.add(new JButton(new AbstractAction(Lang.get("preferences.logging.open"), Icons.get("notes")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                Desktop.getDesktop().open(FileAppender.logfile);
                            } catch (IOException e1) {
                                Logger.error("Unable to open logfile " + FileAppender.logfile, e1);
                            }
                        }
                    }));
                }
                hm.put(a, cb);
            }
        }
        // Legend
        JTextPane legend = new JTextPane();
        legend.setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("preferences.legend")));
        legend.setMaximumSize(new Dimension(9999, 25));
        legend.setFocusable(false);
        legend.setBackground(new Color(getBackground().getRGB()));
        StringBuilder sb = new StringBuilder();
        for (Level l : Level.values()) {
            sb.append(l.toString()).append("\t").append(Lang.getNoHTML("preferences.logging." + l)).append("\n");
        }
        legend.setText(sb.toString());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(comboPanel);
        add(Box.createVerticalGlue());
        add(legend);
    }

    @Override
    public void save() {
        for (Appender a : hm.keySet()) {
            Settings.put("logger." + a.getClass().getSimpleName(), hm.get(a).getSelectedItem().toString());
        }
    }
}
