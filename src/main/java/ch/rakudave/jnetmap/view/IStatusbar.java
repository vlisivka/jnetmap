package ch.rakudave.jnetmap.view;

/**
 * Component usually displayed at the bottom of the window, indicating current activities
 *
 * @author rakudave
 */
public interface IStatusbar {

    /**
     * Erases the message in the Statusbar
     */
    void clearMessage();

    /**
     * Show a busy indicator to indicate an ongoing activity
     * which can not be broken down to a progress in percent
     *
     * @param visible
     */
    void setBusy(boolean visible);

    /**
     * Displays a message in the Statusbar
     *
     * @param message to display
     */
    void setMessage(String message);

    /**
     * Indicate a progress in percent
     * A percentage lower than zero will hide the Progressbar
     *
     * @param percent progress in percent
     */
    void setProgress(int percent);

    void setVisible(boolean visible);

    boolean isVisible();
}
