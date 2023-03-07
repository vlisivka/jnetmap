package ch.rakudave.jnetmap.controller.command;

/**
 * A command that can be re- and undone. Use the constructor to save the "delta"-changes.
 *
 * @author rakudave
 */
public interface Command {

    /**
     * Do or Redo a command. Called by CommandHistory.
     */
    Object redo();

    /**
     * Undo a command. Called by CommandHistory.
     */
    Object undo();

    /**
     * @return the display name of the command
     */
    @Override
    String toString();
}
