package ch.rakudave.jnetmap.controller;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Common listeners
 *
 * @author rakudave
 */
public final class Listeners {

    public static WindowListener windowQuitListener() {
        return new WindowListener() {
            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                Actions.quit(null).actionPerformed(null);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowOpened(WindowEvent e) {
            }
        };
    }
}
