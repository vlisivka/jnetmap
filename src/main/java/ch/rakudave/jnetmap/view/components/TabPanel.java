package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.controller.command.Command;
import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.MapEvent.Type;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.factories.ConnectionFactory;
import ch.rakudave.jnetmap.model.factories.DeviceFactory;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.jung.*;
import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.algorithms.shortestpath.MinimumSpanningForest2;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationServer.Paintable;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import edu.uci.ics.jung.visualization.util.Animator;
import org.apache.commons.collections15.Transformer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Defines a tab-component that contains and draws a map.
 * Tabs feature a close-button similar to Firefox.
 *
 * @author rakudave
 */
@SuppressWarnings("serial")
public class TabPanel extends JTabbedPane implements ChangeListener, GraphMouseListener<Device> {
    private TabPanel _this = this; // parent-reference for inner classes
    private Frame owner;
    private List<GraphMouseListener<Device>> graphListeners;
    private static TabComponent currentTab;
    private JButton plusButton;
    private static ResettableScaler scaler;
    private static List<EditModeListener> editModeListeners = new ArrayList<>();

    public TabPanel(int tabPlacement, int tabLayoutPolicy, Frame owner) {
        super(tabPlacement, tabLayoutPolicy);
        this.owner = owner;
        graphListeners = new ArrayList<>();
        scaler = new ResettableScaler();
        addTab("+", null, new WelcomeScreen());
        createPlusButton();
        //GraphMouse
        ToolBar.getInstance().add("map.pick", getPickingModeSetter());
        ToolBar.getInstance().add("map.edit", getEditingModeSetter());
        ToolBar.getInstance().add("map.transform", getTransformingModeSetter());
        //Zoom
        ToolBar.getInstance().add("map.zoomin", getZoomPlus());
        ToolBar.getInstance().add("map.zoomout", getZoomMinus());
        ToolBar.getInstance().add("map.zoomreset", getZoomReset());
        //ToolBar.getInstance().add("test", getTest());
        addChangeListener(this);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        Object o = getSelectedComponent();
        if (o instanceof TabComponent) {
            currentTab = (TabComponent) o;
            Controller.setCurrentMap(currentTab.map);
            editModeListeners.forEach(eml -> eml.editModeChanged(currentTab.editMode));
        }
    }

    private void closeTab(String tabTitle) {
        int tabIndex = getTabIndex(tabTitle);
        TabComponent tc = (TabComponent) getComponentAt(tabIndex);
        if (!Controller.close(tc.map)) return;
        removeTabAt(tabIndex);
    }

    private void createPlusButton() {
        plusButton = new JButton(Icons.get("add"));
        plusButton.setContentAreaFilled(false);
        plusButton.setBorder(BorderFactory.createEmptyBorder());
        plusButton.addActionListener(e -> openTab(new Map()));
        setTabComponentAt(0, plusButton);
    }

    private int getTabIndex(String tabTitle) {
        for (int i = 0; i < getTabCount() - 1; i++) {
            if (tabTitle.equals(getTabComponentAt(i).getName()))
                return i;
        }
        return -1;
    }

    public void openTab(Map map) {
        int tabCount = getTabCount() - 1;
        if (map.getGraphLayout() == null || map.getGraphLayout().getSize() == null) {
            Logger.error("Invalid graph layout, using fallback");
            map.setLayout(new CircleLayout<>(map));
            map.getGraphLayout().setSize(new Dimension(1000, 1000));
        }
        final VisualizationViewer<Device, Connection> vv = new VisualizationViewer<>(map.getGraphLayout());
        map.addMapListener(e -> {
            if (e.getType() == Type.SETTINGS_CHANGED) {
                vv.getRenderer().getVertexLabelRenderer().setPosition(edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position.S);
                RenderContext<Device, Connection> rc = vv.getRenderContext();
                rc.setVertexIconTransformer(VertexTransformers.iconTransformer(vv.getPickedVertexState()));
                rc.setVertexShapeTransformer(VertexTransformers.shapeTransformer());
                rc.setVertexLabelTransformer(new VertexLabeler());
                rc.setVertexIncludePredicate(map.getDevicePredicate());
                vv.setVertexToolTipTransformer(VertexTransformers.tooltipTransformer());
                if (!Settings.getBoolean("background.transparent", true)) {
                    vv.setBackground(new Color(Settings.getInt("background.color", Color.white.getRGB())));
                }
                rc.setEdgeStrokeTransformer(EdgeTransformers.strokeTransformer());
                rc.setEdgeDrawPaintTransformer(EdgeTransformers.paintTransformer(vv.getPickedEdgeState()));
                rc.setEdgeLabelTransformer(new EdgeLabeler());
                EdgeTransformers.setEdgeShape(rc);
            }
            vv.repaint();
        });
        MapGraphMouse egm = new MapGraphMouse(owner, vv.getRenderContext(), scaler, new DeviceFactory(), new ConnectionFactory(),
                1 / Settings.getFloat("mapview.zoom.scroll", 1.1f), Settings.getFloat("mapview.zoom.scroll", 1.1f));
        egm.setMode(ModalGraphMouse.Mode.PICKING);
        vv.setGraphMouse(egm);
        vv.addGraphMouseListener(_this);
        // background image
        MutableTransformer modelTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
        vv.addPreRenderPaintable(new Paintable() {
            @Override
            public boolean useTransform() {
                return true;
            }

            @Override
            public void paint(Graphics g) {
                if (map.getBackground() != null) {
                    g.drawImage(map.getBackgroundImage(),
                            (int) modelTransformer.getTranslateX(), // no double?
                            (int) modelTransformer.getTranslateY(), null);
                }
            }
        });
        map.refreshView(Type.SETTINGS_CHANGED);
        insertTab(map.getFileName(), null, new TabComponent(map, egm, vv), map.getFilePath(), tabCount);
        setTabComponentAt(tabCount, new ClosableTabHeader(map));
        setSelectedIndex(tabCount);
    }

    public void addGraphListener(GraphMouseListener<Device> l) {
        graphListeners.add(l);
    }

    public void removeGraphListener(GraphMouseListener<Device> l) {
        graphListeners.remove(l);
    }

    @Override
    public void graphClicked(Device v, MouseEvent me) {
        for (GraphMouseListener<Device> l : graphListeners) {
            try {
                l.graphClicked(v, me);
            } catch (Exception e) {
                Logger.error("Unable to notify graphListener", e);
            }
        }
    }

    @Override
    public void graphPressed(Device v, MouseEvent me) {
        for (GraphMouseListener<Device> l : graphListeners) {
            try {
                l.graphPressed(v, me);
            } catch (Exception e) {
                Logger.error("Unable to notify graphListener", e);
            }
        }
    }

    @Override
    public void graphReleased(Device v, MouseEvent me) {
        for (GraphMouseListener<Device> l : graphListeners) {
            try {
                l.graphReleased(v, me);
            } catch (Exception e) {
                Logger.error("Unable to notify graphListener", e);
            }
        }
    }

    public static Action getPickingModeSetter() {
        return new AbstractAction(Lang.getNoHTML("map.pick"), Icons.get("cursor_pick")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTab != null) currentTab.setEditMode(ModalGraphMouse.Mode.PICKING);
            }
        };
    }

    public static Action getEditingModeSetter() {
        return new AbstractAction(Lang.getNoHTML("map.edit"), Icons.get("cursor_edit")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTab != null) currentTab.setEditMode(ModalGraphMouse.Mode.EDITING);
            }
        };
    }

    public static Action getTransformingModeSetter() {
        return new AbstractAction(Lang.getNoHTML("map.transform"), Icons.get("cursor_move")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTab != null) currentTab.setEditMode(ModalGraphMouse.Mode.TRANSFORMING);
            }
        };
    }

    public static Action getZoomPlus() {
        return new AbstractAction(Lang.getNoHTML("map.zoomin"), Icons.get("zoom_in")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTab != null)
                    scaler.scale(currentTab.vv, Settings.getFloat("mapview.zoom.click", 1.5f), currentTab.vv.getCenter());
            }
        };
    }

    public static Action getZoomMinus() {
        return new AbstractAction(Lang.getNoHTML("map.zoomout"), Icons.get("zoom_out")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTab != null)
                    scaler.scale(currentTab.vv, 1 / Settings.getFloat("mapview.zoom.click", 1.5f), currentTab.vv.getCenter());
            }
        };
    }

    public static Action getZoomReset() {
        return new AbstractAction(Lang.getNoHTML("map.zoomreset"), Icons.get("zoom_reset")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTab != null) scaler.reset(currentTab.vv);
            }
        };
    }

    public static Action getZoomTo(Device device) {
        return new AbstractAction(Lang.getNoHTML("map.zoomto"), Icons.get("zoom_reset")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTab != null) {
                    Point2D point = currentTab.vv.getCenter();

                }
            }
        };
    }

    // TODO how to set root of a tree???
    public static Action getTest() {
        final Transformer<Connection, Double> transformer = arg0 -> arg0.getBandwidth();
        return new AbstractAction(Lang.get("test"), Icons.get("add")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                MinimumSpanningForest2<Device, Connection> prim =
                        new MinimumSpanningForest2<>(currentTab.map,
                                new DelegateForest<>(),
                                DelegateTree.getFactory(), transformer);
                //currentTab.vv.setGraphLayout(new TreeLayout<Device, Connection>(prim.getForest()));
                //Forest<Device, Connection> gump = new DelegateForest<Device, Connection>(currentTab.map);
                //TreeLayout<Device, Connection> tree = new TreeLayout<Device, Connection>(gump, 5000, 5000);
                //currentTab.vv.setGraphLayout(tree);
                currentTab.vv.setGraphLayout(new BalloonLayout<>(prim.getForest()));
            }
        };
    }

    @SuppressWarnings({"unchecked"})
    public static List<Action> getLayoutTransformerActions() {
        List<Class> layouts = Arrays.asList(CircleLayout.class, FRLayout.class, ISOMLayout.class, KKLayout.class,
                SpringLayout2.class, GridGraphLayout.class, StaticLayout.class);
        List<Action> actions = new ArrayList<>(layouts.size());
        layouts.forEach(c -> actions.add(new AbstractAction(c.getSimpleName()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setLayout(c);
            }
        }));
        return actions;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setLayout(Class<Layout<Device, Connection>> layout) {
        if (currentTab == null) return;
        final Map map = currentTab.map;
        final VisualizationViewer<Device, Connection> vv = currentTab.vv;
        final Layout<Device, Connection> oldLayout = map.getGraphLayout();
        try {
            Class<? extends Layout<Device, Connection>> layoutC = layout;
            Constructor constructor = layoutC.getConstructor(Graph.class);
            Object o = constructor.newInstance(map);
            final Layout<Device, Connection> newLayout = (Layout<Device, Connection>) o;
            newLayout.setInitializer(vv.getGraphLayout());
            newLayout.setSize(vv.getSize());
            map.getHistory().execute(new Command() {
                @Override
                public Object undo() {
                    map.setLayout(oldLayout);
                    new Animator(new LayoutTransition<>(vv, newLayout, oldLayout)).start();
                    vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
                    vv.repaint();
                    return null;
                }

                @Override
                public Object redo() {
                    map.setLayout(newLayout);
                    new Animator(new LayoutTransition<>(vv, oldLayout, newLayout)).start();
                    vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
                    vv.repaint();
                    return null;
                }

                @Override
                public String toString() {
                    return Lang.getNoHTML("command.set.layout")+": "+layout.getSimpleName();
                }
            });
        } catch (Exception e) {
            Logger.error("Unable to set new layout " + layout.getSimpleName(), e);
        }
    }

    /**
     * "Struct" to keep track of and easily retrieve the Objects corresponding to the displayed Map
     *
     * @author rakudave
     */
    public class TabComponent extends GraphZoomScrollPane {
        Map map;
        MapGraphMouse mouse;
        VisualizationViewer<Device, Connection> vv;
        ModalGraphMouse.Mode editMode = ModalGraphMouse.Mode.PICKING;

        public TabComponent(Map map, MapGraphMouse mouse, VisualizationViewer<Device, Connection> vv) {
            super(vv);
            this.map = map;
            this.mouse = mouse;
            this.vv = vv;
        }

        public void setEditMode(ModalGraphMouse.Mode mode) {
            this.editMode = mode;
            mouse.setMode(mode);
            editModeListeners.forEach(eml -> eml.editModeChanged(currentTab.editMode));
        }
    }

    /**
     * Tab-component that may be closed by clicking the "x" located at the right,
     * similar to the way Firefox does it. It also sports an icon and the usual title and tooltip.
     *
     * @author rakudave
     */
    private class ClosableTabHeader extends JPanel {
        private static final long serialVersionUID = -8130758834224431426L;
        private JButton label, close;

        public ClosableTabHeader(final Map map) {
            super(new BorderLayout());
            setName(map.getFilePath() + "///" + Math.random()); // UID
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            setBackground(new Color(0, 0, 0, 0));
            label = new JButton(map.getFileName(), Icons.get("jnetmap_small")) {{
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
                setContentAreaFilled(false);
                setFocusable(false);
            }};
            label.addActionListener(e -> {
                setSelectedIndex(getTabIndex(getName()));
                setLabel(map.getFileName() + ((map.isSaved()) ? "" : "*"));
            });
            close = new JButton(Icons.get("close")) {{
                setPreferredSize(new Dimension(16, 16));
                setContentAreaFilled(false);
                setFocusable(false);
            }};
            close.addActionListener(e -> _this.closeTab(getName()));
            add(label, BorderLayout.CENTER);
            add(close, BorderLayout.EAST);
            setToolTipText(map.getFilePath());
            map.addMapListener(e -> setLabel(map.getFileName() + ((map.isSaved()) ? "" : "*")));
        }

        public void setLabel(String text) {
            label.setText(text);
        }

        @Override
        public void setToolTipText(String text) {
            super.setToolTipText(text);
            label.setToolTipText(text);
        }
    }

    public static TabComponent getCurrentTab() {
        return currentTab;
    }

    public static void addEditModeListener(EditModeListener listener) {
        editModeListeners.add(listener);
    }
    public static void removeEditModeListener(EditModeListener listener) {
        editModeListeners.remove(listener);
    }
}
