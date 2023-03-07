package ch.rakudave.jnetmap.plugins;

import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.device.DeviceEvent;
import ch.rakudave.jnetmap.plugins.extensions.Notifier;
import ch.rakudave.jnetmap.util.DeviceEventFilter;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.preferences.PreferencePanel;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class SoundNotifierPlugin extends JNetMapPlugin {
    private static Icon icon = Icons.fromBase64("iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyJJREFUOBF1wUtoXFUYB/D/OffcezOZmWYmj0mbp0ZoYiqIURe1ItWFgmhBceGi4EYqrnWTlStdutGdIEUqLiwuFHHl1kdaapOCbWJjmsQ0TSeTmbkz93Fe3+dWAv5+4sy7P+H/RErCM8MYgiWGIF8l4kcDFZTjSF1nZqtwjBCAcwxjCSbwojC+Zi3XSwNyoRKKJ8MAS1luSk6Ki4EUHYVjyNO8N/ad4cFwdrgaPREqTMehjKsVpQYiFU2MlfDzb3v4u2XjE+UICse0O/mFN16YWn7mzAiS1CGMBDwx0lyjnThEsUAYypThPMBQOMYRJ4+cqmBqvIzVjRZaXYNebuEsIy8M4GM4SxAMMDEUhMB/RZHCwVGGKz92it2DdIfIJ3nhms7L/VY3MXMnB9+TsCHbHN7GUEIEYGaACQAjEBBppvHH7YNfO770SqU8aE2ewaaHyLWBKcQlIUzodQIfDEDlB38CMoIarANSwRUpnD2BstI6M9q69g4kMbh/BCosaGyamUiQTkGKoFzWhgxCOMpBIgZpX2g9Cu90aNMUureHtKDnVBAvZkmnlvariOOSEyKAkAoKMkZhCb1eUvXexzpLx4p8AlR0bLe5i8lGbfn9t899PDc9KlbX72P9wOOvtd+L1rUr6IYxlADNjddLb56fm/2gUoqqmzsPZGloDM2tb/Lute8we+GjT+aXzsIWCcIRhYonePtL7Lr3pAOgKjH9cP7s/OKHl17F5l6Cta021nd6YIex8ZPDONq/ffXTz75+a6gxCQGB1v1t5N2t648/tTQY+lyoTvvo6OatTXzx/R30CoK2hPVba2DKaHJm5vW+3vh2/8YD+3Cg/qwQUrJJVrm//eXI1LieGH9Miqeff63WLfgilxrLjcapiTzLdfOftZtV2b4qnL4hme/6gSAvQYwyc5AWurN4eqFpjNVghvLtOx1l6XPd217Z3rVzUhJqlXjDGruzv7t3OFKvo1yfwcsvvdg0/QyXv7qMxrkGHjab6PX7UHfv3UeoAswvnF4pcrfiPIswCFT7sG2dcyBmSCFQG6rBSAm2DtoYEBGkEPgXEF/KiEJoOO8AAAAASUVORK5CYII=");
    private static String pluginName = "Sound Notifier";

    public SoundNotifierPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    @XStreamAlias("ch.rakudave.jnetmap.plugins.SoundNotifier")
    public static class SoundNotifier implements Notifier {
        private String name = "My SoundNotifier";
        private DeviceEventFilter filter = new DeviceEventFilter(true, true);
        private File soundFile;

        @Override
        public void statusChanged(DeviceEvent e, Map m) {
            boolean match = filter.matches(e);
            Logger.debug("Attempting to create log-entry, filtered: " + !match);
            if (!match || soundFile == null) return;
            playSound(soundFile);
        }

        @SuppressWarnings("serial")
        @Override
        public void showPropertiesWindow(final Frame owner, boolean isSetup) {
            final JDialog d = new JDialog(owner, getPluginName() + " - " + name, ModalityType.DOCUMENT_MODAL);
            d.setLayout(new BorderLayout(10, 10));
            final JTextField nameField = new JTextField(name);
            JPanel fileWrapper = new JPanel(new BorderLayout(5, 5));
            fileWrapper.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            final JLabel label = new JLabel(Lang.get("menu.file"));
            final JTextField file = new JTextField((soundFile != null) ? soundFile.getAbsolutePath() : "");
            final Color defaultColor = new Color(file.getForeground().getRGB());
            file.addFocusListener(new FocusListener() {
                @Override
                public void focusLost(FocusEvent e) {
                }

                @Override
                public void focusGained(FocusEvent e) {
                    label.setForeground(defaultColor);
                }
            });
            JPanel buttonWrapper = new JPanel(new GridLayout(1, 2, 5, 5));
            JButton browse = new JButton(new AbstractAction(Lang.get("action.browse")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    java.util.List<String> names = new ArrayList<>();
                    java.util.List<String> extensions = new ArrayList<>();
                    Arrays.stream(AudioSystem.getAudioFileTypes()).forEach(type -> {
                        names.add(type.toString());
                        extensions.add(type.getExtension());
                    });
                    File f = SwingHelper.saveDialog(owner, new FileNameExtensionFilter(
                            String.join(", ", names), extensions.toArray(new String[0])));
                    if (f != null) {
                        file.setText(f.getAbsolutePath());
                    }
                }
            });
            JButton test = new JButton(new AbstractAction(Lang.get("action.test")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    playSound(new File(file.getText()));
                }
            });
            buttonWrapper.add(browse);
            buttonWrapper.add(test);
            fileWrapper.add(label, BorderLayout.WEST);
            fileWrapper.add(file, BorderLayout.CENTER);
            fileWrapper.add(buttonWrapper, BorderLayout.EAST);
            JPanel centerWrapper = new JPanel();
            centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.PAGE_AXIS));
            centerWrapper.add(fileWrapper);
            centerWrapper.add(filter.settingsPanel());
            JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
            JButton cancel = new JButton(Lang.get("action.cancel"), Icons.get("cancel"));
            cancel.addActionListener(e -> d.dispose());
            JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
            ok.setPreferredSize(cancel.getPreferredSize());
            ok.addActionListener(e -> {
                name = nameField.getText();
                if (!file.getText().isEmpty()) {
                    try {
                        soundFile = new File(file.getText());
                        if (soundFile.exists() && soundFile.canRead()) {
                            d.dispose();
                            if (name.isEmpty()) name = soundFile.getName();
                            return;
                        }
                    } catch (Exception e2) {
                        Logger.error("No such file: " + file, e2);
                    }
                }
                label.setForeground(Color.red);
                soundFile = null;
            });
            if (!isSetup) bottomRow.add(cancel);
            bottomRow.add(ok);
            d.add(nameField, BorderLayout.NORTH);
            d.add(centerWrapper, BorderLayout.CENTER);
            d.add(bottomRow, BorderLayout.SOUTH);
            d.pack();
            SwingHelper.centerTo(owner, d);
            d.setVisible(true);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getPluginName() {
            return pluginName;
        }

        @Override
        public Icon getIcon() {
            return icon;
        }

        @Override
        public Notifier create() {
            return new SoundNotifier();
        }

        private void playSound(File file) {
            try {
                Clip clip = AudioSystem.getClip();
                clip.open(AudioSystem.getAudioInputStream(file));
            } catch (Exception e) {
                Logger.warn("Failed to play sound", e);
            }
        }
    }
    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public String getAuthor() {
        return "rakudave";
    }

    @Override
    public String getDescription() {
        return "Plays a sound when a device/interface comes online or goes offline.";
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    @Override
    public PreferencePanel getSettingsPanel() {
        return null;
    }

}
