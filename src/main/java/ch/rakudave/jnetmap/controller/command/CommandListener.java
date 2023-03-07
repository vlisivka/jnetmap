package ch.rakudave.jnetmap.controller.command;

public interface CommandListener {
    void executed(Command c);
    void undone(Command command);
    void redone(Command command);
}
