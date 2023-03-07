package ch.rakudave.jnetmap.view.jung;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.controller.Scheduler;
import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.plugins.extensions.RightClickAction;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.components.StatusBar;
import ch.rakudave.jnetmap.view.components.TabPanel;
import ch.rakudave.jnetmap.view.preferences.ScriptsPanel;
import ch.rakudave.jnetmap.view.properties.ConnectionProperties;
import ch.rakudave.jnetmap.view.properties.DeviceProperties;
import ch.rakudave.jnetmap.view.properties.InterfaceProperties;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;
import edu.uci.ics.jung.visualization.picking.PickedState;
import org.apache.commons.collections15.Factory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * a plugin that uses popup menus to create vertices, undirected edges, and
 * directed edges.
 *
 * @author Tom Nelson, rakudave
 */
public class PopupGraphMousePlugin extends AbstractPopupGraphMousePlugin {
    private Frame owner;
    private Factory<Device> vertexFactory;
    private JMenu plugins;
    private Device vertex;


    public PopupGraphMousePlugin(Frame owner, Factory<Device> vertexFactory) {
        this.owner = owner;
        this.vertexFactory = vertexFactory;
        buildPluginsMenu();
    }

    @SuppressWarnings("serial")
    private void buildPluginsMenu() {
        plugins = new JMenu(Lang.get("preferences.plugins"));
        plugins.setIcon(Icons.get("plugin"));
        List<RightClickAction> rightClickPlugins = Controller.getPluginManager().getExtensions(RightClickAction.class);
        rightClickPlugins.addAll(ScriptsPanel.getScriptPlugins());
        rightClickPlugins.sort(Comparator.comparing(RightClickAction::getName));
        for (final RightClickAction p : rightClickPlugins) {
            plugins.add(new AbstractAction(p.getName(), p.getIcon()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        Scheduler.execute(() -> p.execute(vertex));
                    } catch (Exception ex) {
                        Logger.error("An error occured in plugin '" + p.getName() + "'", ex);
                    }
                }
            });
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "serial"})
    protected void handlePopup(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        final VisualizationViewer<Device, Connection> vv = (VisualizationViewer<Device, Connection>) e
                .getSource();
        final Layout<Device, Connection> layout = vv.getGraphLayout();
        final Graph<Device, Connection> graph = layout.getGraph();
        final Point2D p = e.getPoint();
        final Point2D ivp = p;
        GraphElementAccessor<Device, Connection> pickSupport = vv.getPickSupport();
        if (pickSupport != null) {
            vertex = pickSupport.getVertex(layout, ivp.getX(), ivp.getY());
            final Connection edge = pickSupport.getEdge(layout, ivp.getX(), ivp.getY());
            final PickedState<Device> pickedVertexState = vv.getPickedVertexState();
            final PickedState<Connection> pickedEdgeState = vv.getPickedEdgeState();

            if (vertex != null) {
                pickedVertexState.pick(vertex, false);
                popup.add(new AbstractAction(Lang.get("device.properties"), Icons.get("properties")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        new DeviceProperties(owner, vertex, false);
                    }
                });
                if (ScriptsPanel.isDirty()) buildPluginsMenu();
                if (plugins.getMenuComponentCount() > 0) popup.add(plugins);
                popup.add(new AbstractAction(Lang.get("menu.view.refresh"), Icons.get("refresh")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // TODO should be a triggerable action instead of being hard-coded here
                        Scheduler.execute(() -> {
                            StatusBar.getInstance().setBusy(true);
                            StatusBar.getInstance().setMessage(Lang.getNoHTML("message.status.update").replaceAll("%name%", vertex.getName()));
                            vertex.updateStatus();
                            try {
                                Collection<Device> neighbors = Controller.getCurrentMap().getNeighbors(vertex);
                                if (!neighbors.isEmpty()) for (Device d : neighbors) d.updateStatus();
                            } catch (Exception e1) {
                                Logger.error("Failed to update the neighbors of " + vertex, e1);
                            }
                            StatusBar.getInstance().setBusy(false);
                            StatusBar.getInstance().clearMessage();
                            TabPanel.getCurrentTab().repaint();
                        });
                    }
                });
                //popup.add(TabPanel.getZoomTo(vertex));
                popup.add(new AbstractAction(Lang.get("action.delete"), Icons.get("remove")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        graph.removeVertex(vertex);
                        vv.repaint();
                    }
                });
            } else if (edge != null) {
                pickedEdgeState.pick(edge, false);
                popup.add(new AbstractAction(Lang.get("connection.properties"), Icons.get("properties")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Pair<Device> p = Controller.getCurrentMap().getEndpoints(edge);
                        if (p.getFirst().equals(p.getSecond())) {
                            new InterfaceProperties(owner, p.getFirst().getInterfaceFor(edge));
                        } else {
                            new ConnectionProperties(owner, edge);
                        }
                    }
                });
                popup.add(new AbstractAction(Lang.get("menu.view.refresh"), Icons.get("refresh")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        edge.updateStatus();
                    }
                });
                popup.add(new AbstractAction(Lang.get("action.delete"), Icons.get("remove")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        graph.removeEdge(edge);
                        vv.repaint();
                    }
                });
            } else {
                popup.add(new AbstractAction(Lang.get("action.add"), Icons.get("add")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Device newVertex = vertexFactory.create();
                        new DeviceProperties(owner, newVertex, true);
                        graph.addVertex(newVertex);
                        layout.setLocation(newVertex, vv.getRenderContext()
                                .getMultiLayerTransformer().inverseTransform(p));
                        vv.repaint();
                    }
                });
            }
            if (popup.getComponentCount() > 0) {
                popup.show(vv, e.getX(), e.getY());
            }
        }
    }
}