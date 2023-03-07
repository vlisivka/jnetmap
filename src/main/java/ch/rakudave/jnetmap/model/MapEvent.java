package ch.rakudave.jnetmap.model;

import java.util.EventObject;

/**
 * @author rakudave
 */
@SuppressWarnings("serial")
public final class MapEvent extends EventObject {
    public enum Type {EDGE_ADDED, EDGE_REMOVED, VERTEX_ADDED, VERTEX_REMOVED, DEVICE_EVENT, REFRESH, SETTINGS_CHANGED, EDGE_CHANGED, SAVED}

    private Type type;
    private Object subject;

    /**
     * Event for changes that occur on a map, e.g. added items etc...
     *
     * @param source  where the event came from
     * @param subject of the event, e.g. item that has changed
     */
    public MapEvent(Map source, Type t, Object subject) {
        super(source);
        type = t;
        this.subject = subject;
    }

    /**
     * @return the reason for this event, if any (null otherwise)
     */
    public Object getSubject() {
        return subject;
    }

    /**
     * @return the type of event
     */
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return getSource() + "/" + type + "/" + getSubject();
    }

}
