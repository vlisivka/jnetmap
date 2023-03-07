package ch.rakudave.jnetmap.view.jung;

import ch.rakudave.jnetmap.util.Settings;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.control.ViewScalingControl;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

import java.awt.geom.Point2D;

public class ResettableScaler extends ViewScalingControl {

    @Override
    public void scale(VisualizationServer vv, float amount, Point2D from) {
        MutableTransformer viewTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW);
        if ((viewTransformer.getScale() > Settings.getFloat("mapview.zoom.max", 2f) && amount > 1) ||
                (viewTransformer.getScale() < Settings.getFloat("mapview.zoom.min", 0.2f) && amount < 1)) return;
        super.scale(vv, amount, from);
    }

    public void reset(VisualizationServer<?, ?> vv) {
        MutableTransformer layoutTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
        MutableTransformer viewTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW);
        layoutTransformer.setToIdentity();
        viewTransformer.setToIdentity();
    }
}
