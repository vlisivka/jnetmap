package ch.rakudave.jnetmap.controller.command;

import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;

import java.util.*;


/**
 * @author rakudave
 */
public final class CommandHistory {
    private List<CommandListener> listeners = new ArrayList<>();
    private LinkedList<Command> done = new LinkedList<>(), undone = new LinkedList<>();

    public Object execute(Command c) {
        try {
            Object result = c.redo();
            done.addFirst(c);
            undone.clear();
            if (done.size() > Settings.getInt("commandhistory.size", 20)) done.removeLast();
            listeners.forEach(l -> l.executed(c));
            return result;
        } catch (Exception e) {
            Logger.error("Unable to execute command", e);
            return null;
        }
    }

    public boolean canRedo() {
        return undone.size() > 0;
    }

    public Object redo() {
        if (!canRedo()) return null;
        try {
            Command c = undone.getFirst();
            Object result = c.redo();
            undone.removeFirst();
            done.addFirst(c);
            listeners.forEach(l -> l.redone(c));
            return result;
        } catch (Exception e) {
            Logger.error("Unable to redo command", e);
            return null;
        }
    }

    public int getRedoSize() {
        return undone.size();
    }

    public boolean canUndo() {
        return done.size() > 0;
    }

    public Object undo() {
        if (!canUndo()) return null;
        try {
            Command c = done.getFirst();
            Object result = c.undo();
            done.removeFirst();
            undone.addFirst(c);
            listeners.forEach(l -> l.undone(c));
            return result;
        } catch (Exception e) {
            Logger.error("Unable to undo command", e);
            return null;
        }
    }

    public int getUndoSize() {
        return done.size();
    }

    public Vector<Command> getCommands() {
        Vector<Command> result = new Vector<>();
        result.addAll(undone);
        Collections.reverse(result);
        result.addAll(done);
        return result;
    }

    public void addCommandListener(CommandListener listener) {
        listeners.add(listener);
    }
    public void removeCommandListener(CommandListener listener) {
        listeners.remove(listener);
    }
}


