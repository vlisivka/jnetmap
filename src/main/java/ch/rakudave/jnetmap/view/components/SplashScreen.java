package ch.rakudave.jnetmap.view.components;

import javax.swing.*;
import java.awt.*;

/**
 * Shows a borderless window containing an image and a progress-bar
 *
 * @author rakudave
 */
public class SplashScreen {
    private static JFrame frame;
    private static JProgressBar progress;

    public static void create() {
        if (frame != null) return;
        final JLabel l = new JLabel(new ImageIcon(SplashScreen.class.getResource("/img/splash.png")));
        progress = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        progress.setStringPainted(true);
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final Dimension labelSize = l.getPreferredSize();
        frame = new JFrame();
        frame.setTitle("jNetMap - loading...");
        frame.setIconImage(new ImageIcon(SplashScreen.class.getResource("/icons/Tango/jnetmap.png")).getImage());
        frame.setResizable(false);
        frame.setEnabled(false);
        frame.setUndecorated(true);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().add(l, BorderLayout.CENTER);
        frame.getContentPane().add(progress, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocation(screenSize.width / 2 - (labelSize.width / 2), screenSize.height / 2 - (labelSize.height / 2));
        frame.setVisible(true);
    }

    /**
     * Dispose this SplashScreen
     */
    public static void dispose() {
        if (frame != null) frame.dispose();
        frame = null;
    }

    /**
     * Set the progress (when loading components)
     *
     * @param percent of progress
     */
    public static void setProgress(int percent) {
        if (progress != null) progress.setValue(percent);
    }

    /**
     * Set the progress (when loading components) and an accompanying message
     *
     * @param percent of progress
     * @param message to show
     */
    public static void setProgress(int percent, String message) {
        setProgress(percent);
        setProgress(message);
    }

    /**
     * Set the message showing in the progress-bar
     *
     * @param message to show
     */
    public static void setProgress(String message) {
        if (progress != null) progress.setString(message);
    }

}
