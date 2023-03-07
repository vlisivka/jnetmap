package ch.rakudave.jnetmap.view.jung;

import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.Host;
import ch.rakudave.jnetmap.net.status.Status;
import ch.rakudave.jnetmap.util.Icons;
import edu.uci.ics.jung.visualization.FourPassImageShaper;
import edu.uci.ics.jung.visualization.decorators.EllipseVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.VertexIconShapeTransformer;
import edu.uci.ics.jung.visualization.picking.PickedState;
import org.apache.commons.collections15.Transformer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

/**
 * Custom JUNG transformers for vertices
 *
 * @author rakudave
 */
public class VertexTransformers {

    public static Transformer<Device, Icon> iconTransformer(final PickedState<Device> pi) {
        return d -> getIcon(d, (pi.isPicked(d)) ? "_s" : "");

    }

    private static Icon getIcon(Device d, String flag) {
        String type = d.getType();
        if (type != null) {
            if (type.equals(Host.otherType)) {
                ImageIcon img = Icons.fromBase64(d.getOtherID());
                if (img != null) return img;
            } else {
                return Icons.getCisco(type.toString().toLowerCase() + flag);
            }
        }
        return Icons.getCisco("workstation" + flag);
    }

    public static Transformer<Device, Shape> shapeTransformer() {
        return new DeviceVertexIconShapeTransformer<>(new EllipseVertexShapeTransformer<>());
    }

    public static Transformer<Device, String> tooltipTransformer() {
        return arg0 -> {
            List<NetworkIF> nifs = arg0.getInterfaces();
            if (nifs.isEmpty()) return "---";
            int i = 0;
            StringBuilder sb = new StringBuilder("<html>");
            for (NetworkIF nif : nifs) {
                sb.append(nif.getName());
                if (nif.getAddress() != null) {
                    sb.append(": ").append(nif.getAddress().getHostAddress());
                    if (Status.UP.equals(nif.getStatus())) sb.append(" (").append(nif.getLatency()).append("ms)");
                }
                if (++i != nifs.size()) sb.append("<br />");
            }
            return sb.append("</html>").toString();
        };
    }

    public static class DeviceVertexIconShapeTransformer<V> extends VertexIconShapeTransformer<V> {

        public DeviceVertexIconShapeTransformer(Transformer<V, Shape> delegate) {
            super(delegate);
        }

        @Override
        public Shape transform(V v) {
            Icon icon = getIcon((Device) v, "");
            if (icon != null && icon instanceof ImageIcon) {
                Image image = ((ImageIcon) icon).getImage();
                Shape shape = shapeMap.get(image);
                if (shape == null) {
                    shape = FourPassImageShaper.getShape(image, 32);
                    if (shape.getBounds().getWidth() > 0 && shape.getBounds().getHeight() > 0) {
                        AffineTransform transform =
                                AffineTransform.getTranslateInstance(-image.getWidth(null) / 2, -image.getHeight(null) / 2);
                        shape = transform.createTransformedShape(shape);
                        shapeMap.put(image, shape);
                    }
                }
                return shape;
            } else {
                return delegate.transform(v);
            }
        }
    }
}
