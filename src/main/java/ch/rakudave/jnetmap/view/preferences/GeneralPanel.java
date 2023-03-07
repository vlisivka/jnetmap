package ch.rakudave.jnetmap.view.preferences;

import ch.rakudave.jnetmap.util.*;
import ch.rakudave.jnetmap.util.logging.Logger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author rakudave
 */
@SuppressWarnings("serial")
public class GeneralPanel extends PreferencePanel {
    @SuppressWarnings("rawtypes")
    private JComboBox languageSelector;
    private SpinnerNumberModel undoSize, historySize, updateThreads;
    private JTextField dateFormat;
    private JLabel dateFormatPreview;
    private boolean isDateValid = true;
    private JCheckBox restoreMaps, restordeWH, restordeXY, queryArp, dotDesktopFile;
    private JRadioButton viewNormal, viewMinimized, viewMaximized;


    @SuppressWarnings({"unchecked", "rawtypes"})
    public GeneralPanel(final Window parent) {
        title = Lang.getNoHTML("preferences.general");
        // Language
        JPanel languageWrapper = new JPanel(new GridLayout(1, 2, 5, 5)) {{
            setMaximumSize(new Dimension(9999, 25));
            setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("preferences.general.language")));
            languageSelector = new JComboBox(Lang.getLanguages());
            languageSelector.setSelectedItem(Lang.currentLanguage());
            add(languageSelector);
            add(SwingHelper.createAlignedButton(new AbstractAction(Lang.get("action.install"), Icons.get("install")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    File f = SwingHelper.openDialog(parent, null);
                    if (f == null) return;
                    IO.copy(f, new File(IO.langDirName, f.getName()), null);
                    languageSelector.setModel(new DefaultComboBoxModel(Lang.getLanguages()));
                    languageSelector.setSelectedItem(Lang.currentLanguage());
                    languageSelector.showPopup();
                }
            }));
        }};
        // Restore
        JPanel restorePanel = new JPanel(new GridLayout(0, 1, 5, 5));
        restorePanel.setMaximumSize(new Dimension(9999, 60));
        restorePanel.setBorder(new TitledBorder(Lang.getNoHTML("preferences.appearance.window")));
        JPanel viewState = new JPanel(new GridLayout(1, 0, 5, 5));
        viewNormal = new JRadioButton(Lang.get("preferences.appearance.viewstate.normal"), Settings.getInt("view.extendedstate", 0) == Frame.NORMAL);
        viewMaximized = new JRadioButton(Lang.get("preferences.appearance.viewstate.maximized"), Settings.getInt("view.extendedstate", 0) == Frame.MAXIMIZED_BOTH);
        viewMinimized = new JRadioButton(Lang.get("preferences.appearance.viewstate.minimized"), Settings.getInt("view.extendedstate", 0) == Frame.ICONIFIED);
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(viewNormal);
        viewState.add(viewNormal);
        viewGroup.add(viewMaximized);
        viewState.add(viewMaximized);
        viewGroup.add(viewMinimized);
        viewState.add(viewMinimized);
        viewNormal.addChangeListener(e -> {
            restordeWH.setEnabled(viewNormal.isSelected());
            restordeXY.setEnabled(viewNormal.isSelected());
        });
        restordeWH = new JCheckBox(Lang.get("preferences.appearance.remember.wh"), Settings.getBoolean("mapview.remember.wh", true));
        restordeWH.setEnabled(viewNormal.isSelected());
        restordeXY = new JCheckBox(Lang.get("preferences.appearance.remember.xy"), Settings.getBoolean("mapview.remember.xy", true));
        restordeXY.setEnabled(viewNormal.isSelected());
        restorePanel.add(viewState);
        restorePanel.add(restordeWH);
        restorePanel.add(restordeXY);
        JPanel miscWrapper = new JPanel(new GridLayout(0, 2, 5, 5));
        miscWrapper.setMaximumSize(new Dimension(9999, 90));
        // Undo history size
        miscWrapper.add(new JLabel(Lang.get("preferences.general.undosize")));
        undoSize = new SpinnerNumberModel(Settings.getInt("commandhistory.size", 20), 5, 100, 1);
        miscWrapper.add(new JSpinner(undoSize));
        // Undo history size
        miscWrapper.add(new JLabel(Lang.get("preferences.general.historysize")));
        historySize = new SpinnerNumberModel(Settings.getInt("device.history.maxsize", 20), 5, 100, 1);
        miscWrapper.add(new JSpinner(historySize));
        // Update threads
        miscWrapper.add(new JLabel(Lang.get("preferences.general.updatethreads")));
        updateThreads = new SpinnerNumberModel(Settings.getInt("status.update.threads", 5), 1, 20, 1);
        miscWrapper.add(new JSpinner(updateThreads));
        // Date format
        miscWrapper.add(new JLabel(Lang.get("preferences.general.dateformat")));
        dateFormat = new JTextField(Settings.get("view.dateformat", "yyyy-MM-dd HH:mm"));
        miscWrapper.add(dateFormat);
        miscWrapper.add(new JLabel(""));
        dateFormatPreview = new JLabel("");
        miscWrapper.add(dateFormatPreview);
        dateFormat.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                try {
                    dateFormatPreview.setText(new SimpleDateFormat(dateFormat.getText()).format(new Date()));
                    isDateValid = true;
                } catch (Exception e) {
                    Logger.debug("Invalid date format", e);
                    dateFormatPreview.setText("---");
                    isDateValid = false;
                }
            }
            @Override
            public void keyPressed(KeyEvent keyEvent) {}
            @Override
            public void keyReleased(KeyEvent keyEvent) {}
        });
        // Checkboxes
        JPanel cbWrapper = new JPanel(new GridLayout(0, 1, 5, 5));
        cbWrapper.setMaximumSize(new Dimension(9999, 90));
        restoreMaps = new JCheckBox(Lang.get("maps.restore"), Settings.getBoolean("maps.restore", false));
        cbWrapper.add(restoreMaps);
        queryArp = new JCheckBox(Lang.get("preferences.general.arp"), Settings.getBoolean("arp.query", true));
        cbWrapper.add(queryArp);
        if (IO.isLinux) {
            dotDesktopFile = new JCheckBox(Lang.get("preferences.general.dotdesktopfile"), Settings.getBoolean("recent.desktop.file", true));
            cbWrapper.add(dotDesktopFile);
        }

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(languageWrapper);
        add(Box.createVerticalStrut(5));
        add(restorePanel);
        add(Box.createVerticalStrut(5));
        add(miscWrapper);
        add(Box.createVerticalStrut(5));
        add(cbWrapper);
        add(Box.createVerticalGlue());
    }

    @Override
    public String getParentTitle() {
        return (parentTitle == null) ? "" : parentTitle;
    }

    @Override
    public String getTitle() {
        return (title == null) ? "" : title;
    }

    @Override
    public void save() {
        Settings.put("lang", languageSelector.getSelectedItem().toString());
        Settings.put("view.extendedstate", viewMaximized.isSelected() ? Frame.MAXIMIZED_BOTH : (viewMinimized.isSelected() ? Frame.ICONIFIED : Frame.NORMAL));
        Settings.put("mapview.remember.wh", restordeWH.isSelected());
        Settings.put("mapview.remember.xy", restordeXY.isSelected());
        Settings.put("status.update.threads", updateThreads.getNumber().intValue());
        if (isDateValid) Settings.put("view.dateformat", dateFormat.getText());
        Settings.put("commandhistory.size", undoSize.getNumber().intValue());
        Settings.put("device.history.maxsize", historySize.getNumber().intValue());
        Settings.put("maps.restore", restoreMaps.isSelected());
        Settings.put("arp.query", queryArp.isSelected());
        if (IO.isLinux) Settings.put("recent.desktop.file", dotDesktopFile.isSelected());
    }
}
