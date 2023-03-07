package ch.rakudave.jnetmap.util.logging;

import ch.rakudave.jnetmap.util.logging.Logger.Level;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author rakudave
 */
public class ListenerAppender extends Appender {
    private ChangeListener listener;

    public ListenerAppender(Level level, ChangeListener listener) {
        setLevel(level);
        this.listener = listener;
    }

    @Override
    void append(Level l, String message, Throwable t) {
        if (level.compareTo(l) >= 0) {
            listener.stateChanged(new ChangeEvent(format(l, message) + "\n" + ((t != null) ? t.toString() : "")));
        }
    }
}
