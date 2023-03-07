package ch.rakudave.jnetmap.controller;

import ch.rakudave.jnetmap.util.IO;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class RecentlyOpened implements MenuListener {
    private static Set<File> history = new HashSet<>();
    private JMenu menu;

    public RecentlyOpened(JMenu parent) {
        menu = parent;
    }

    public static void load() {
        history.clear();
        int i = 0;
        while (true) {
            String path = Settings.get("recent." + i++, null);
            if (path == null) break;
            else if (new File(path).exists()) history.add(new File(path));
        }
    }

    public static void save() {
        int i = 0;
        Settings.removeAll("recent.");
        for (File f : history) {
            Settings.put("recent." + i++, f.getAbsolutePath());
        }

        if (!IO.isLinux) return;
        File dotDesktopFile = new File("/usr/share/applications/jnetmap.desktop");
        File targetDir = new File(System.getProperty("user.home") + "/.local/share/applications");
        if (dotDesktopFile.exists() && targetDir.exists() && Settings.getBoolean("recent.desktop.file", true)) {
            StringBuilder sb = new StringBuilder();
            StringBuilder actions = new StringBuilder();
            sb.append(IO.getString(dotDesktopFile));
            int j = 1;
            for (File file : history) {
                if (file == null || !file.exists()) continue;
                String id = "recent"+j++;
                actions.append(id).append(";");
                sb.append("\n[Desktop Action ").append(id).append("]\n");
                sb.append("Name=").append(file.getName()).append("\n");
                sb.append("Exec=jnetmap \"").append(file.getAbsolutePath()).append("\"\n");
            }
            String search = "Actions=new;open;";
            sb.insert(sb.indexOf(search)+search.length(), actions.toString());
            File customDesktop = new File(targetDir, "jnetmap.desktop");
            try (Writer w = new FileWriter(customDesktop)) {
                w.write(sb.toString());
                w.flush();
                customDesktop.setExecutable(true);
            } catch (IOException e) {
                Logger.debug("Failed to write custom .destop file", e);
            }
        }
    }

    public static void put(File file) {
        history.add(file);
    }

    public static void clear() {
        history.clear();
    }

    public static Set<File> getAll() {
        return Collections.unmodifiableSet(history);
    }

    @Override
    public void menuSelected(MenuEvent e) {
        menu.removeAll();
        for (File file : history) {
            if (file == null || !file.exists()) continue;
            JMenuItem item = new JMenuItem(new AbstractAction(file.getName()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Actions.open(file, null);
                }
            });
            item.setToolTipText(file.getAbsolutePath());
            menu.add(item);
        }
        menu.add(new JSeparator());
        menu.add(new JMenuItem(new AbstractAction(Lang.getNoHTML("menu.file.recent.clear"), Icons.get("remove")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                history.clear();
            }
        }));
    }

    @Override
    public void menuDeselected(MenuEvent e) {
    }

    @Override
    public void menuCanceled(MenuEvent e) {
    }
}
