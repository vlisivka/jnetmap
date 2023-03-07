package ch.rakudave.jnetmap.plugins;

import ch.rakudave.jnetmap.view.preferences.PreferencePanel;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import javax.swing.*;


/**
 * Denotes a jNetMap-Plugin. Must offer information that will be displayed in the preferences and the respective menus.
 *
 * @author rakudave
 */
public abstract class JNetMapPlugin extends Plugin implements Comparable<JNetMapPlugin> {

    public JNetMapPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * @return the name of this plugins
     */
    public abstract String getPluginName();

    /**
     * @return the author of this plugin
     */
    public abstract String getAuthor();

    /**
     * @return the description of this plugin, displayed in the settings window
     */
    public abstract String getDescription();

    /**
     * @return the icon of this plugin
     * (hint: you may use the built-in Base64-decoder so you don't have to ship extra resources)
     */
    public abstract Icon getIcon();

    /**
     * @return true if this plugins offers a settings-panel
     */
    public abstract boolean hasSettings();

    /**
     * @return the settings-panel of this plugins that should be displayed in the settings window, may return null if hasSettings() is false
     */
    public abstract PreferencePanel getSettingsPanel();

    @Override
    public int compareTo(JNetMapPlugin jNetMapPlugin) {
        if (getPluginName() == null || jNetMapPlugin == null) return 0;
        return getPluginName().compareTo(jNetMapPlugin.getPluginName());
    }
}
