package ch.rakudave.jnetmap.controller;

import ch.rakudave.jnetmap.controller.command.CommandHistory;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;
import ch.rakudave.jnetmap.util.logging.FileAppender;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.components.StatusBar;
import ch.rakudave.jnetmap.view.preferences.Preferences;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;

/**
 * @author rakudave
 */
@SuppressWarnings("serial")
public class Actions {
    public static final String URL_MAIN = "https://rakudave.ch/jnetmap";
    public static final String URL_CONTACT = "https://rakudave.ch/jnetmap/#contact";
    public static final String URL_PLUGIN = "https://rakudave.ch/jnetmap/#download";
    public static final String URL_GUIDE = "https://rakudave.ch/wp-content/uploads/2021/05/jNetMap_0.5_User_Guide.pdf";
    public static final String URL_BUGS = "https://sourceforge.net/p/jnetmap/bugs";
    public static final String URL_FEATURE = "https://sourceforge.net/p/jnetmap/feature-requests";
    public static final String URL_MAIL = "jnetmap@rakudave.ch";

    // New file
    public static Action newMap(String label) {
        return new AbstractAction(label, Icons.get("new")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Controller.getView().openMap(new Map());
            }
        };
    }

    // Open file
    public static Action open(String label) {
        return new AbstractAction(label, Icons.get("open")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] o = SwingHelper.openDialog((Component) Controller.getView(), new FileNameExtensionFilter("jNetMap file *.jnm", "jnm"), true);
                if (o == null) return;
                File f = (File) o[0];
                if (f == null) return;
                open(f, (String) o[1]);
            }
        };
    }

    public static void open(File f, String password) {
        Map map = Controller.getMapFromFile(f.getAbsolutePath(), password);
        if (map != null) {
            Controller.open(map);
        } else {
            JOptionPane.showMessageDialog((Component) Controller.getView(), Lang.getNoHTML("message.fail.open"),
                    Lang.getNoHTML("message.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    // Save file
    public static Action save() {
        return new AbstractAction(Lang.getNoHTML("menu.file.save"), Icons.get("save")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Map map = Controller.getCurrentMap();
                if (map == null) {
                    Logger.trace("nothing to save");
                } else if (Lang.getNoHTML("map.unsaved").equals(map.getFilePath())) {
                    saveAs().actionPerformed(null);
                } else {
                    if (!map.save()) {
                        JOptionPane.showMessageDialog((Component) Controller.getView(), Lang.getNoHTML("message.fail.save"),
                                Lang.getNoHTML("message.error"), JOptionPane.ERROR_MESSAGE);
                    } else {
                        StatusBar.getInstance().setMessage(Lang.getNoHTML("menu.file.saved") + ": " + map.getFilePath());
                    }
                }
            }
        };
    }

    // Save As...
    public static Action saveAs() {
        return new AbstractAction(Lang.getNoHTML("menu.file.saveas"), Icons.get("save-as")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Map map = Controller.getCurrentMap();
                if (map == null) return;
                Object[] o = SwingHelper.saveDialog((Component) Controller.getView(), new FileNameExtensionFilter("jNetMap file *.jnm", "jnm"), true);
                if (o == null) return;
                File f = (File) o[0];
                if (f == null) return;
                map.setFile(f);
                map.setPassword((String) o[1]);
                if (!map.save()) {
                    JOptionPane.showMessageDialog((Component) Controller.getView(), Lang.getNoHTML("message.fail.save"),
                            Lang.getNoHTML("message.error"), JOptionPane.ERROR_MESSAGE);
                } else {
                    RecentlyOpened.put(f);
                }
            }
        };
    }

    // Show preferences dialog
    public static Action preferences(String label) {
        return new AbstractAction(label, Icons.get("preferences")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Preferences(Controller.getView());
            }
        };
    }

    // Quit
    public static Action quit(String label) {
        return new AbstractAction(label, Icons.get("quit")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Controller.shutdown(false);
            }
        };
    }

    // Restart
    public static Action restart(String label) {
        return new AbstractAction(label, Icons.get("refresh")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Controller.shutdown(true);
            }
        };
    }

    // Show documentation
    public static Action viewDoc(String label) {
        return new AbstractAction(label, Icons.get("help")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Scheduler.execute(() -> openWebsite(URL_GUIDE));
            }
        };
    }

    // Show website
    public static Action viewWebsite() {
        return new AbstractAction(Lang.getNoHTML("menu.help.website"), Icons.get("jnetmap_small")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Scheduler.execute(() -> openWebsite(URL_MAIN));
            }
        };
    }

    // report a bug
    public static Action reportBug() {
        return new AbstractAction(Lang.getNoHTML("menu.help.bug"), Icons.get("bug")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Scheduler.execute(() -> openWebsite(URL_BUGS));
            }
        };
    }

    // request a feature
    public static Action requestFeature() {
        return new AbstractAction(Lang.getNoHTML("menu.help.feature"), Icons.get("add")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Scheduler.execute(() -> openWebsite(URL_FEATURE));
            }
        };
    }

    // contact developer
    public static Action contactDeveloper() {
        return new AbstractAction(Lang.getNoHTML("menu.help.contact"), Icons.get("mail")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Scheduler.execute(() -> {
                    String mail = "mailto:" + URL_MAIL + "?subject=jNetMap-" + Controller.version.replaceAll(" ", "-");
                    try {
                        mail += "&attachment=" + URLEncoder.encode(FileAppender.logfile.getAbsolutePath(), "UTF-8");
                        Desktop.getDesktop().mail(new URI(mail));
                    } catch (Exception ex) {
                        Logger.error("Failed to open mail-client: " + mail, ex);
                        openWebsite(URL_CONTACT);
                    }
                });
            }
        };
    }

    public static void openWebsite(String url) {
        try {
            Logger.trace("Opening website: " + url);
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            Logger.error("Failed to open website: " + url, ex);
        }
    }

    public static Action undo() {
        return new AbstractAction(Lang.getNoHTML("menu.undo"), Icons.get("undo")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                CommandHistory history = Controller.getCurrentMap().getHistory();
                try {
                    history.undo();
                } catch (Exception ex) {
                    Logger.error("An error occured during the last 'undo'", ex);
                }
            }
        };
    }

    public static Action redo() {
        return new AbstractAction(Lang.getNoHTML("menu.redo"), Icons.get("redo")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                CommandHistory history = Controller.getCurrentMap().getHistory();
                try {
                    history.redo();
                } catch (Exception ex) {
                    Logger.error("An error occured during the last 'redo'", ex);
                }
            }
        };
    }

    public static Action refresh() {
        return new AbstractAction(Lang.getNoHTML("menu.view.refresh"), Icons.get("refresh")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Controller.getCurrentMap() == null) return;
                Scheduler.execute(() -> StatusUpdater.refresh(Controller.getCurrentMap()));
            }
        };
    }

    private Actions() {
    }
}
