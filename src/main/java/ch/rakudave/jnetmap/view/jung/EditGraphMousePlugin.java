package ch.rakudave.jnetmap.view.jung;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.view.properties.ConnectionProperties;
import ch.rakudave.jnetmap.view.properties.DeviceProperties;
import ch.rakudave.jnetmap.view.properties.InterfaceProperties;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractGraphMousePlugin;
import org.apache.commons.collections15.Factory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;

/**
 * A plugin that can create vertices, undirected edges, and directed edges using
 * mouse gestures.
 *
 * @author Tom Nelson
 * @author rakudave
 */
public class EditGraphMousePlugin extends AbstractGraphMousePlugin implements
        MouseListener, MouseMotionListener {

    protected Device startVertex;
    protected Point2D down;

    protected CubicCurve2D rawEdge = new CubicCurve2D.Float();
    protected Shape edgeShape;
    protected Shape rawArrowShape;
    protected Shape arrowShape;
    protected VisualizationServer.Paintable edgePaintable;
    protected VisualizationServer.Paintable arrowPaintable;
    protected EdgeType edgeIsDirected;
    protected Factory<Device> vertexFactory;
    protected Factory<Connection> edgeFactory;
    private Frame owner;

    public EditGraphMousePlugin(Frame owner, Factory<Device> vertexFactory,
                                Factory<Connection> edgeFactory) {
        this(owner, MouseEvent.BUTTON1_MASK, vertexFactory, edgeFactory);
    }

    /**
     * create instance and prepare shapes for visual effects
     *
     * @param modifiers
     */
    public EditGraphMousePlugin(Frame owner, int modifiers,
                                Factory<Device> vertexFactory, Factory<Connection> edgeFactory) {
        super(modifiers);
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        rawEdge.setCurve(0.0f, 0.0f, 0.33f, 100, .66f, -50, 1.0f, 0.0f);
        edgePaintable = new EdgePaintable();
        arrowPaintable = new ArrowPaintable();
        this.cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        this.owner = owner;
    }

    /**
     * Overridden to be more flexible, and pass events with key combinations.
     * The default responds to both ButtonOne and ButtonOne+Shift
     */
    @Override
    public boolean checkModifiers(MouseEvent e) {
        return (e.getModifiers() & modifiers) != 0;
    }

    /**
     * If the mouse is pressed in an empty area, create a new vertex there. If
     * the mouse is pressed on an existing vertex, prepare to create an edge
     * from that vertex to another
     */
    @SuppressWarnings("unchecked")
    public void mousePressed(MouseEvent e) {
        if (checkModifiers(e)) {
            final VisualizationViewer<Device, Connection> vv = (VisualizationViewer<Device, Connection>) e
                    .getSource();
            final Point2D p = e.getPoint();
            GraphElementAccessor<Device, Connection> pickSupport = vv
                    .getPickSupport();
            if (pickSupport != null) {
                Graph<Device, Connection> graph = vv.getModel()
                        .getGraphLayout().getGraph();
                // set default edge type
                edgeIsDirected = EdgeType.UNDIRECTED;

                final Device vertex = pickSupport.getVertex(vv.getModel()
                        .getGraphLayout(), p.getX(), p.getY());
                if (vertex != null) { // get ready to make an edge
                    startVertex = vertex;
                    down = e.getPoint();
                    transformEdgeShape(down, down);
                    vv.addPostRenderPaintable(edgePaintable);
                } else { // make a new vertex
                    Device newVertex = vertexFactory.create();
                    Layout<Device, Connection> layout = vv.getModel()
                            .getGraphLayout();
                    graph.addVertex(newVertex);
                    layout.setLocation(newVertex, vv.getRenderContext()
                            .getMultiLayerTransformer().inverseTransform(
                                    e.getPoint()));
                    new DeviceProperties(owner, newVertex, true);
                }
            }
            vv.repaint();
        }
    }

    /**
     * If startVertex is non-null, and the mouse is released over an existing
     * vertex, create an undirected edge from startVertex to the vertex under
     * the mouse pointer. If shift was also pressed, create a directed edge
     * instead.
     */
    @SuppressWarnings("unchecked")
    public void mouseReleased(MouseEvent e) {
        if (checkModifiers(e)) {
            final VisualizationViewer<Device, Connection> vv = (VisualizationViewer<Device, Connection>) e
                    .getSource();
            final Point2D p = e.getPoint();
            Layout<Device, Connection> layout = vv.getModel().getGraphLayout();
            GraphElementAccessor<Device, Connection> pickSupport = vv
                    .getPickSupport();
            if (pickSupport != null) {
                final Device vertex = pickSupport.getVertex(layout, p.getX(), p.getY());
                if (vertex != null && startVertex != null) {
                    Graph<Device, Connection> graph = vv.getGraphLayout().getGraph();
                    Connection newEdge = edgeFactory.create();
                    graph.addEdge(newEdge, startVertex, vertex, edgeIsDirected);
                    if (startVertex.equals(vertex)) {
                        new InterfaceProperties(owner, vertex.getInterfaceFor(newEdge));
                    } else {
                        new ConnectionProperties(owner, newEdge);
                    }
                    vv.repaint();
                }
            }
            startVertex = null;
            down = null;
            edgeIsDirected = EdgeType.UNDIRECTED;
            vv.removePostRenderPaintable(edgePaintable);
            vv.removePostRenderPaintable(arrowPaintable);
        }
    }

    /**
     * If startVertex is non-null, stretch an edge shape between startVertex and
     * the mouse pointer to simulate edge creation
     */
    @SuppressWarnings("unchecked")
    public void mouseDragged(MouseEvent e) {
        if (checkModifiers(e)) {
            if (startVertex != null) {
                transformEdgeShape(down, e.getPoint());
                if (edgeIsDirected == EdgeType.DIRECTED) {
                    transformArrowShape(down, e.getPoint());
                }
            }
            VisualizationViewer<Device, Connection> vv = (VisualizationViewer<Device, Connection>) e
                    .getSource();
            vv.repaint();
        }
    }

    /**
     * code lifted from PluggableRenderer to move an edge shape into an
     * arbitrary position
     */
    private void transformEdgeShape(Point2D down, Point2D out) {
        float x1 = (float) down.getX();
        float y1 = (float) down.getY();
        float x2 = (float) out.getX();
        float y2 = (float) out.getY();

        AffineTransform xform = AffineTransform.getTranslateInstance(x1, y1);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float thetaRadians = (float) Math.atan2(dy, dx);
        xform.rotate(thetaRadians);
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        xform.scale(dist / rawEdge.getBounds().getWidth(), 1.0);
        edgeShape = xform.createTransformedShape(rawEdge);
    }

    private void transformArrowShape(Point2D down, Point2D out) {
        float x1 = (float) down.getX();
        float y1 = (float) down.getY();
        float x2 = (float) out.getX();
        float y2 = (float) out.getY();

        AffineTransform xform = AffineTransform.getTranslateInstance(x2, y2);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float thetaRadians = (float) Math.atan2(dy, dx);
        xform.rotate(thetaRadians);
        arrowShape = xform.createTransformedShape(rawArrowShape);
    }

    /**
     * Used for the edge creation visual effect during mouse drag
     */
    class EdgePaintable implements VisualizationServer.Paintable {

        public void paint(Graphics g) {
            if (edgeShape != null) {
                Color oldColor = g.getColor();
                g.setColor(Color.black);
                ((Graphics2D) g).draw(edgeShape);
                g.setColor(oldColor);
            }
        }

        public boolean useTransform() {
            return false;
        }
    }

    /**
     * Used for the directed edge creation visual effect during mouse drag
     */
    class ArrowPaintable implements VisualizationServer.Paintable {

        public void paint(Graphics g) {
            if (arrowShape != null) {
                Color oldColor = g.getColor();
                g.setColor(Color.black);
                ((Graphics2D) g).fill(arrowShape);
                g.setColor(oldColor);
            }
        }

        public boolean useTransform() {
            return false;
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        JComponent c = (JComponent) e.getSource();
        c.setCursor(cursor);
    }

    public void mouseExited(MouseEvent e) {
        JComponent c = (JComponent) e.getSource();
        c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void mouseMoved(MouseEvent e) {
    }
}