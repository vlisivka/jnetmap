package ch.rakudave.jnetmap.net.status;

import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.awt.*;

/**
 * @author rakudave
 */
@XStreamAlias("Status")
public enum Status {
    UNKNOWN(Lang.getNoHTML("message.status.unknown"), Settings.getInt("status.color.unknown", -8355712)),
    DOWN(Lang.getNoHTML("message.status.down"), Settings.getInt("status.color.down", -65536)),
    NOT_FOUND(Lang.getNoHTML("message.status.notfound"), Settings.getInt("status.color.not_found", -14336)),
    UP(Lang.getNoHTML("message.status.up"), Settings.getInt("status.color.up", -16711936));

    private String message;
    private Color color;

    Status(String message, int color) {
        this.message = message;
        this.color = new Color(color);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color c) {
        color = c;
        Settings.put("status.color." + toString().toLowerCase(), color.getRGB());
    }

    public String getMessage() {
        return message;
    }

    public String getHtmlValue() {
        String rgb = Integer.toHexString(color.getRGB());
        return "#" + rgb.substring(2, rgb.length());
    }
}
