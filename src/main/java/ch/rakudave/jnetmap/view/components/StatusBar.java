package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.view.IStatusbar;

import javax.swing.*;
import java.awt.*;

public class StatusBar extends JPanel implements IStatusbar {
    private static final long serialVersionUID = -5510132656286800042L;
    private static StatusBar instance;
    private static final Object lock = new Object();
    private JLabel label, busyIndicator;
    private JProgressBar progress;

    public static IStatusbar getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new StatusBar();
                }
            }
        }
        return instance;
    }

    @SuppressWarnings("serial")
    private StatusBar() {
        super(new BorderLayout());
        setVisible(Settings.getBoolean("statusbar.visible", true));
        busyIndicator = new JLabel(Icons.get("busy")) {{
            setBorder(BorderFactory.createEmptyBorder(2, 5, 0, 0));
            setVisible(false);
        }};
        add(busyIndicator, BorderLayout.WEST);
        label = new JLabel() {{
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        }};
        add(label, BorderLayout.CENTER);
        progress = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100) {{
            setStringPainted(true);
            setVisible(false);
        }};
        add(progress, BorderLayout.EAST);
    }

    @Override
    public void clearMessage() {
        label.setText("\t");
    }

    @Override
    public void setBusy(boolean busy) {
        busyIndicator.setVisible(busy);
    }

    @Override
    public void setMessage(String message) {
        label.setText(message);
    }

    @Override
    public void setProgress(int percent) {
        progress.setValue(percent);
        progress.setVisible(percent >= 0);
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        Settings.put("statusbar.visible", aFlag);
    }
}
