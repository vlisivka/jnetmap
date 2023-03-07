package ch.rakudave.jnetmap.view.jung;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.util.Settings;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.picking.PickedInfo;
import org.apache.commons.collections15.Transformer;

import java.awt.*;


/**
 * Custom JUNG transformers for edges
 *
 * @author rakudave
 */
public class EdgeTransformers {
    public enum Shape {Line, Curve, Quad, Bent}
    private static Color selectedColor = new Color(Settings.getInt("edge.selected.color", Color.black.getRGB()));

    public static Transformer<Connection, Stroke> strokeTransformer() {
        return connection -> getStroke(connection.getType(), connection.getBandwidth());
    }

    public static BasicStroke getStroke(Connection.Type type, double bandwidth) {
        float[] dash;
        switch (type) {
            case Coaxial:
                dash = new float[]{20, 10, 5, 10};
                break;
            case Fiber:
                dash = new float[]{20, 10};
                break;
            case Phone:
                dash = new float[]{20, 7, 2, 7};
                break;
            case Serial:
                dash = new float[]{20, 7, 50, 7};
                break;
            case Wireless:
                dash = new float[]{5, 10};
                break;
            default:
                dash = new float[]{1};
                break;
        }
        return new BasicStroke(((float) Math.log(bandwidth)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dash, 0f);
    }

    public static Transformer<Connection, Paint> paintTransformer(final PickedInfo<Connection> pi) {
        return arg0 -> {
            if (pi.isPicked(arg0)) {
                if (Settings.getInt("edge.selected.color", Color.black.getRGB()) != selectedColor.getRGB()) {
                    selectedColor = new Color(Settings.getInt("edge.selected.color", Color.black.getRGB()));
                }
                return selectedColor;
            } else {
                return arg0.getStatus().getColor();
            }
        };
    }

    public static void setEdgeShape(RenderContext<Device, Connection> rc) {
        switch (Shape.valueOf(Settings.get("edge.shape", "Quad"))) {
            case Line:
                rc.setEdgeShapeTransformer(new EdgeShape.Line<>());
                break;
            case Curve:
                rc.setEdgeShapeTransformer(new EdgeShape.CubicCurve<>());
                break;
            case Bent:
                rc.setEdgeShapeTransformer(new EdgeShape.BentLine<>());
                break;
            default:
                rc.setEdgeShapeTransformer(new EdgeShape.QuadCurve<>());
                break;
        }
    }
}
