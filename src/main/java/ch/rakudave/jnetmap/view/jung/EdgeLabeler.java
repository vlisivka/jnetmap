package ch.rakudave.jnetmap.view.jung;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.net.status.Status;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.SwingHelper;
import org.apache.commons.collections15.Transformer;

public class EdgeLabeler implements Transformer<Connection, String> {

    @Override
    public String transform(Connection c) {
        StringBuilder sb = new StringBuilder();
        if (Settings.getBoolean("connection.label.name", false)) {
            if (c.getName() != null) sb.append(c.getName());
        }
        if (Settings.getBoolean("connection.label.status", false)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(c.getStatus().getMessage());
        }
        if (Settings.getBoolean("connection.label.latency", false) && Status.UP.equals(c.getStatus())) {
            if (sb.length() > 0) sb.append(", ");
            Long latency = c.getLatency();
            if (latency != null) sb.append(latency).append("ms");
        }
        return sb.insert(0, "<html><center style=\""+ SwingHelper.getViewFontAsCss() +";\">")
                .append("</center></html>").toString();
    }

}
