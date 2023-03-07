package ch.rakudave.jnetmap.view.jung;

import ch.rakudave.jnetmap.util.Settings;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.graph.Graph;

import java.awt.geom.Point2D;
import java.util.HashMap;

public class GridGraphLayout<V, E> extends AbstractLayout<V, E> {

    public GridGraphLayout(Graph<V, E> graph) {
        super(graph);
    }

    @Override
    public void initialize() {
        int gridSize = Settings.getInt("gridlayout.size", 128);
        HashMap<Double, Double> locations = new HashMap<>();
        for (V v : getGraph().getVertices()) {
            Point2D coord = transform(v);
            double x = roundToGrid(gridSize, coord.getX()), y = roundToGrid(gridSize, coord.getY());
            while (true) {
                Double otherY = locations.get(x);
                if (otherY != null && y == otherY) {
                    if (x < 1280) {
                        x += gridSize;
                    } else {
                        y += gridSize;
                    }
                } else {
                    locations.put(x, y);
                    coord.setLocation(x, y);
                    break;
                }
            }
        }
    }

    @Override
    public void reset() {
    }

    private double roundToGrid(double gridSize, double n) {
        double offset = n % gridSize;
        if (offset > gridSize / 2) {
            return n - offset + gridSize;
        } else {
            return n - offset;
        }

    }
}
