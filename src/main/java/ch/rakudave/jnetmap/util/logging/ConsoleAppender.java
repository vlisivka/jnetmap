package ch.rakudave.jnetmap.util.logging;

import ch.rakudave.jnetmap.util.logging.Logger.Level;

/**
 * @author rakudave
 */
public class ConsoleAppender extends Appender {
    private boolean colorize = true;

    public ConsoleAppender(Level level) {
        setLevel(level);
    }

    public boolean isColorize() {
        return colorize;
    }

    public void setColorize(boolean colorize) {
        this.colorize = colorize;
    }

    @Override
    void append(Level l, String message, Throwable t) {
        if (level.compareTo(l) >= 0) {
            System.out.println(format(l, message));
            if (t != null) t.printStackTrace();
        }
    }

    String format(Level l, String message) {
        return df.format(System.currentTimeMillis()) + (colorize ? colorize(l) : equalizeLevel(l)) + message;
    }

    static final String ANSI_RESET = "\u001B[0m";
    static final String ANSI_BLACK = "\u001B[30m";
    static final String ANSI_RED = "\u001B[31m";
    static final String ANSI_GREEN = "\u001B[32m";
    static final String ANSI_YELLOW = "\u001B[33m";
    static final String ANSI_BLUE = "\u001B[34m";
    static final String ANSI_PURPLE = "\u001B[35m";
    static final String ANSI_CYAN = "\u001B[36m";
    static final String ANSI_WHITE = "\u001B[37m";
    static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
    static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
    static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";
    private String colorize(Level l) {
        StringBuilder sb = new StringBuilder();
        switch (l) {
            case FATAL:
                sb.append(ANSI_RED_BACKGROUND);
                break;
            case ERROR:
                sb.append(ANSI_RED);
                break;
            case WARN:
                sb.append(ANSI_YELLOW);
                break;
            case DEBUG:
                sb.append(ANSI_BLUE);
                break;
            case TRACE:
                sb.append(ANSI_GREEN);
                break;
            default:
                break;
        }
        return sb.append(equalizeLevel(l)).append(ANSI_RESET).toString();
    }
}