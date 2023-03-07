package ch.rakudave.jnetmap.view.preferences;

import javax.swing.*;

/**
 * @author rakudave
 */
@SuppressWarnings("serial")
public abstract class PreferencePanel extends JPanel {
    protected String title, parentTitle;

    /**
     * @return The name of its parent, for building the Tree. An empty String means there's no parent.
     */
    public String getParentTitle() {
        return (parentTitle == null) ? "" : parentTitle;
    }

    /**
     * @return A displayable name for this panel
     */
    public String getTitle() {
        return (title == null) ? "" : title;
    }

    /**
     * This method will be called by the "Preferences"-window once the OK-Button is pressed, so you can write
     * all changes to in this Panel to the settings-file.
     *
     * @see ch.rakudave.jnetmap.util.Settings
     */
    public abstract void save();
}