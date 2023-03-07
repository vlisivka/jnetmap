package ch.rakudave.jnetmap.model.device;

import java.util.Date;
import java.util.EventObject;


/**
 * @author rakudave
 */
@SuppressWarnings("serial")
public final class DeviceEvent extends EventObject {
    public enum Type {STATUS_CHANGED, PROPERTY_CHANGED, INTERFACE_STATUS_CHANGED, INTERFACE_ADDED, INTERFACE_REMOVED}

    private Type type;
    private Object subject;
    private Date date;

    public DeviceEvent() {
        super(null);
    }

    public DeviceEvent(Device source, Type type, Object subject) {
        super(source);
        this.type = type;
        this.subject = subject;
        date = new Date();
    }

    /**
     * @return device that caused the event
     */
    public Device getItem() {
        return (Device) getSource();
    }

    /**
     * @return type of this event
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the subject of the event, if any (null otherwise)
     */
    public Object getSubject() {
        return subject;
    }

    /**
     * @return the time and date the event occurred
     */
    public Date getTime() {
        return date;
    }

    @Override
    public String toString() {
        return getSource() + "/" + type + "/" + getSubject() + " at " + date.toString();
    }
}
