package ch.rakudave.jnetmap.util.logging;

import ch.rakudave.jnetmap.util.logging.Logger.Level;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author rakudave
 */
public abstract class Appender {
    protected final DateFormat df = new SimpleDateFormat("HH:mm:ss");
    protected Level level;

    abstract void append(Level l, String message, Throwable t);

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level l) {
        if (l != null) level = l;
    }

    String format(Level l, String message) {
        return df.format(System.currentTimeMillis()) + equalizeLevel(l) + message;
    }

    protected String equalizeLevel(Level l) {
        return String.format("%6s: ", l);
    }
}