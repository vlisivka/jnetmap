package ch.rakudave.jnetmap.model.factories;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.Connection.Type;
import org.apache.commons.collections15.Factory;

public class ConnectionFactory implements Factory<Connection> {
    private static Type type = Type.Ethernet;
    private static double speed = 100;

    public ConnectionFactory() {
    }

    @Override
    public Connection create() {
        return new Connection(type, speed);
    }

    public static void setType(Type t) {
        if (t != null) type = t;
    }

    public static void setSpeed(double s) {
        speed = s;
    }
}
