package ch.rakudave.jnetmap.controller;

import ch.rakudave.jnetmap.model.CurrentMapListener;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.MapEvent.Type;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.util.*;
import ch.rakudave.jnetmap.util.logging.FileAppender;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.util.logging.Logger.Level;
import ch.rakudave.jnetmap.view.IView;
import ch.rakudave.jnetmap.view.MapView;
import ch.rakudave.jnetmap.view.components.StatusBar;
import org.apache.commons.cli.*;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author rakudave
 */
public class Controller {
    public static final String version = "0.5.6";
    // TODO de-statify this and use DI instead
    private static IView view;
    private static PluginManager pluginManager;
    private static List<Map> maps;
    private static Map currentMap;
    private static List<CurrentMapListener> currentMapListeners;

    public static void main(String[] args) {
        try {
            Options options = new Options();
            options.addOption("i", "new-instance", false, "launch new instance instead of trying to delegate");
            options.addOption("n", "new", false, "open a new map");
            options.addOption("o", "open", false, "open a file chooser to select a map to be opened");
            options.addOption("c", "close", false, "close an already running instance and exit");
            options.addOption("v", "version", false, "display the version number and exit");
            options.addOption("h", "help", false, "display this message and exit");

            CommandLine cmd = new DefaultParser().parse(options, args);
            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("jnetmap [OPTIONS] [files]", options);
                System.exit(0);
            } else if (cmd.hasOption("v")) {
                System.out.println(version);
                System.exit(0);
            } else if (GraphicsEnvironment.isHeadless()) {
                Logger.fatal("No graphics environment found, exiting");
                System.exit(1);
            }
            if (!cmd.hasOption("i")) InstanceDetector.delegate(cmd);
            new Controller(cmd);
        } catch (Throwable t) {
            Logger.error("Unhandled exception", t);
            t.printStackTrace();
        }
    }

    private Controller(CommandLine cmd) {
        Scheduler.executor = new ScheduledThreadPoolExecutor(3);
        maps = new ArrayList<>();
        currentMapListeners = new ArrayList<>();
        IO.updateUserFiles();
        Settings.load();
        Logger.addAppender(new FileAppender(Level.valueOf(Settings.get("logger.FileAppender", "ERROR"))));
        Logger.debug(System.getProperty("java.runtime.name") + " " + System.getProperty("java.version") + " (" + System.getProperty("sun.arch.data.model") + "bit)");
        Logger.debug("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
        Lang.load();
        Icons.load();
        setLookAndFeel();
        try {
            //System.setProperty("pf4j.pluginsDir", new File(IO.userDir, IO.pluginDirName).getAbsolutePath());
            pluginManager = new DefaultPluginManager(new File(IO.userDir, IO.pluginDirName).toPath());
            pluginManager.loadPlugins(); // see https://github.com/decebals/pf4j
            pluginManager.startPlugins();
        } catch (Exception e) {
            Logger.fatal("Failed to load plugins", e);
        }
        RecentlyOpened.load();
        view = new MapView();
        StatusBar.getInstance().setMessage(Lang.get("message.status.welcome"));
        openRequestedFiles(cmd, false);
        Logger.info("Ready");
        view.toFront();
    }

    private void setLookAndFeel() {
        try {
            if (IO.isOSX) System.setProperty("apple.laf.useScreenMenuBar", "true");
            UIManager.setLookAndFeel(Settings.get("laf.theme", "com.formdev.flatlaf.FlatIntelliJLaf"));
            SwingHelper.setUIFont(null);
        } catch (Exception e) {
            Logger.warn("Unable to set 'Look-And-Feel', falling back to Swing", e);
        }
    }

    static void openRequestedFiles(CommandLine cmd, boolean isDelegate) {
        if (view != null) view.toFront();
        if (cmd.hasOption("n")) {
                Controller.getView().openMap(new Map());
        }
        if (cmd.hasOption("o")) {Object[] o = SwingHelper.openDialog((Component) Controller.getView(),
                new FileNameExtensionFilter("jNetMap file *.jnm", "jnm"), true);
            if (o != null && o.length > 0 && o[0] != null) open(getMapFromFile(((File) o[0]).getAbsolutePath(), null));
        }

        List<String> files = cmd.getArgList(); // treat remaining args as files
        if (files.isEmpty() && !isDelegate && Settings.getBoolean("maps.restore", false)) {
            for (int i = 0; i < Settings.getInt("maps.count", 0); i++) {
                files.add(Settings.get("maps." + i, ""));
            }
        }
        files.forEach(file -> {
            StatusBar.getInstance().setMessage("Opening "+file+" ...");
            open(getMapFromFile(file, null));
        });
    }

    public static boolean open(Map map) {
        if (map == null) return false;
        Logger.info("Opening '" + map.getFileName() + "'");
        if (maps.contains(map)) {
            Logger.warn("Map is already open: " + map.getFileName());
            return true;
        }
        try {
            StatusBar.getInstance().setMessage(Lang.getNoHTML("message.status.opening").replace("%name%", map.getFileName()));
            view.openMap(map);
            setCurrentMap(map);
            return true;
        } catch (Exception e) {
            Logger.error("Unable to open " + map.getFileName(), e);
            StatusBar.getInstance().clearMessage();
            JOptionPane.showMessageDialog((Component) Controller.view, Lang.get("map.fail.open"), map.getFileName(), JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public static Map getMapFromFile(String filePath, String password) {
        try {
            File mapFile = new File(filePath);
            String xml = IO.getString(mapFile);
            if (xml == null || xml.isEmpty()) throw new Exception("File '"+filePath+"' was empty");
            if (!xml.startsWith("<Map>") && (password == null || password.isEmpty())) {
                password = SwingHelper.passwordPrompt();
                if (password == null) return null;
            }
            if (password != null && !password.isEmpty()) {
                xml = Crypto.decrypt(xml, password);
            }
            Map m = (Map) XStreamHelper.getXStream().fromXML(xml);
            m.setFile(mapFile);
            m.setPassword(password);
            if (Settings.getBoolean("maps.saved_on_open", true)) m.setSaved(true);
            RecentlyOpened.put(mapFile);
            return m;
        } catch (Exception e) {
            Logger.error("Could not open file '" + filePath + "'", e);
            return null;
        }
    }

    public static boolean close(Map map) {
        if (map == null || !maps.contains(map)) return true;
        Logger.trace("Closing map " + map);
        if (!map.isSaved()) {
            String msg = Lang.get("message.unsaved").replaceAll("%name%", map.getFileName());
            int i = JOptionPane.showConfirmDialog((Component) view, msg,
                    map.getFilePath(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            switch (i) {
                case 2:
                    return false;
                case 0:
                    setStatusUnknownAndSave(map);
                    break;
                default:
                    break;
            }
        } else {
            setStatusUnknownAndSave(map);
        }
        maps.remove(map);
        return true;
    }

    private static void setStatusUnknownAndSave(Map map) {
        for (Device d : map.getVertices()) d.addStatusUnknownToHistory();
        setCurrentMap(map);
        Actions.save().actionPerformed(null);
    }

    public static void setCurrentMap(Map map) {
        if (!maps.contains(currentMap)) {
            maps.add(map);
            StatusUpdater.addMap(map);
        }
        currentMap = map;
        view.setWindowTitle(map.getFilePath());
        for (CurrentMapListener listener : currentMapListeners) {
            try {
                listener.mapChanged(map);
            } catch (Throwable t) {
                Logger.warn("Failed to notify currentMapListener", t);
            }
        }
    }

    public static Map getCurrentMap() {
        return currentMap;
    }

    public static void addCurrentMapListener(CurrentMapListener listener) {
        currentMapListeners.add(listener);
    }

    public static void removeCurrentMapListener(CurrentMapListener listener) {
        currentMapListeners.remove(listener);
    }

     private static boolean closeAllMaps() {
        List<Map> tempMaps = new ArrayList<>(maps);
        boolean restore = Settings.getBoolean("maps.restore", false);
        Settings.removeAll("maps.");
        int i = 0;
        for (Map m : tempMaps) {
            if (!close(m)) return false;
            if (!m.getFileName().equals(Lang.getNoHTML("map.newmap")))
                Settings.put("maps." + i++, m.getFilePath());
        }
         Settings.put("maps.count", i);
         Settings.put("maps.restore", restore);
        return true;
    }

    static void shutdown(boolean restart) {
        if (!closeAllMaps()) return;
        RecentlyOpened.save();
        view.saveViewProperties();
        Settings.save();
        view.dispose();
        Logger.info("Shutting down...");
        Scheduler.executor.shutdown();
        try {
            Scheduler.executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.debug("Shutdown interrupted", e);
        }
        if (restart) {
            try {
                new Controller(new DefaultParser().parse(new Options(), new String[0]));
            } catch (ParseException e) {
                Logger.error("Failed to restart", e);
            }
        } else {
            System.exit(0);
        }

    }

    public static IView getView() {
        return view;
    }

    public static void refreshAll() {
        for (Map m : maps) m.refreshView(Type.SETTINGS_CHANGED);
    }

    public static PluginManager getPluginManager() {
        return pluginManager;
    }
}
