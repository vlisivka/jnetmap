package ch.rakudave.jnetmap.util.logging;

import ch.rakudave.jnetmap.util.Settings;

import java.util.ArrayList;
import java.util.List;


/**
 * (very) minimalistic 'implementation' of log4j
 *
 * @author rakudave
 */
public class Logger {
    public enum Level {OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE}

    private static List<Appender> appenders = init();

    public static void addAppender(Appender a) {
        if (a != null) appenders.add(a);
    }

    public static void debug(String message) {
        log(Level.DEBUG, message, null);
    }

    public static void debug(String message, Throwable t) {
        log(Level.DEBUG, message, t);
    }

    public static void error(String message) {
        log(Level.ERROR, message, null);
    }

    public static void error(String message, Throwable t) {
        log(Level.ERROR, message, t);
    }

    public static void fatal(String message) {
        log(Level.FATAL, message, null);
    }

    public static void fatal(String message, Throwable t) {
        log(Level.FATAL, message, t);
    }

    public static List<Appender> getAppenders() {
        return new ArrayList<>(appenders);
    }

    public static void info(String message) {
        log(Level.INFO, message, null);
    }

    public static void info(String message, Throwable t) {
        log(Level.INFO, message, t);
    }

    // Initialize appenders and add the universal sysout-appender
    // (Other Appenders might need some config and should be added later)
    private static List<Appender> init() {
        appenders = new ArrayList<>();
        appenders.add(new ConsoleAppender(Level.valueOf(Settings.get("logger.ConsoleAppender", "WARN"))));
        return appenders;
    }

    public static void log(Level level, String message, Throwable t) {
        for (Appender a : appenders) {
            a.append(level, message, t);
        }
    }

    public static void trace(String message) {
        log(Level.TRACE, message, null);
    }

    public static void trace(String message, Throwable t) {
        log(Level.TRACE, message, t);
    }

    public static void warn(String message) {
        log(Level.WARN, message, null);
    }

    public static void warn(String message, Throwable t) {
        log(Level.WARN, message, t);
    }
}
