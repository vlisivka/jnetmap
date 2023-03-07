package ch.rakudave.jnetmap.model;

import ch.rakudave.jnetmap.controller.Scheduler;
import ch.rakudave.jnetmap.controller.command.Command;
import ch.rakudave.jnetmap.controller.command.CommandHistory;
import ch.rakudave.jnetmap.model.IF.LogicalIF;
import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.IF.PhysicalIF;
import ch.rakudave.jnetmap.model.IF.TransparentIF;
import ch.rakudave.jnetmap.model.MapEvent.Type;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.model.device.DeviceEvent;
import ch.rakudave.jnetmap.model.device.DeviceListener;
import ch.rakudave.jnetmap.net.status.Status;
import ch.rakudave.jnetmap.plugins.extensions.Notifier;
import ch.rakudave.jnetmap.util.*;
import ch.rakudave.jnetmap.util.logging.Logger;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.MultiGraph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import org.apache.commons.collections15.Predicate;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author rakudave
 */
@XStreamAlias("Map")
public class Map implements MultiGraph<Device, Connection>, Graph<Device, Connection>, DirectedGraph<Device, Connection>, DeviceListener {
    private Map _this = this;
    @XStreamOmitField
    private File mapFile;
    @XStreamOmitField
    private String password = "";
    @XStreamOmitField
    private CommandHistory history;
    private double updateInterval = 5;
    private Graph<Device, Connection> graph;
    private Layout<Device, Connection> layout;
    @XStreamOmitField
    private List<MapListener> mapListeners;
    @XStreamConverter(SkippingCollectionConverter.class)
    private List<Notifier> statusListeners;
    private File background;
    @XStreamOmitField
    private Image backgroundImage;
    private Set<Layer> layers;
    @XStreamOmitField
    private boolean saved = true;


    public Map() {
        history = new CommandHistory();
        graph = new SparseMultigraph<>();
        layout = new StaticLayout<>(this);
        layout.setSize(new Dimension(1000, 1000));
        mapListeners = new ArrayList<>();
        statusListeners = new ArrayList<>();
        layers = new LinkedHashSet<>();
    }

    public boolean addMapListener(MapListener listener) {
        if (listener == null) return false;
        if (mapListeners == null) mapListeners = new ArrayList<>();
        return mapListeners.add(listener);
    }

    public boolean removeMapListener(MapListener listener) {
        return !(listener == null || mapListeners == null) && mapListeners.remove(listener);
    }

    public boolean addStatusListener(Notifier listener) {
        if (listener == null) return false;
        if (statusListeners == null) statusListeners = new ArrayList<>();
        return statusListeners.add(listener);
    }

    public boolean removeStatusListener(Notifier listener) {
        return !(listener == null || statusListeners == null) && statusListeners.remove(listener);
    }

    public List<Notifier> getStatusListeners() {
        return statusListeners;
    }

    private void notifyListeners(final MapEvent e) {
        Logger.trace("Received MapEvent " + e);
        if (mapListeners == null) mapListeners = new ArrayList<>(); //???
        for (MapListener l : mapListeners) {
            try {
                l.mapChanged(e);
            } catch (Exception ex) {
                Logger.error("Unable to notify MapListener " + l, ex);
            }
        }
        if (e.getType() == Type.DEVICE_EVENT) {
            Logger.trace("Received DeviceEvent " + e);
            Scheduler.execute(() -> {
                DeviceEvent ev = (DeviceEvent) e.getSubject();
                if (ev.getType() == DeviceEvent.Type.STATUS_CHANGED || ev.getType() == DeviceEvent.Type.INTERFACE_STATUS_CHANGED) {
                    for (Notifier l : statusListeners) {
                        try {
                            l.statusChanged(ev, _this);
                        } catch (Exception e2) {
                            Logger.error("Unable to notify StatusListener " + l, e2);
                        }
                    }
                }
            });
        }
        if (e.getType() != Type.REFRESH) {
            refreshView(null);
            if (e.getType() != Type.SAVED && e.getType() != Type.SETTINGS_CHANGED) setSaved(false);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Map other = (Map) obj;
        return !(mapFile == null || other.mapFile == null) && mapFile.equals(other.mapFile);
    }

    public String getFileName() {
        return (mapFile != null) ? mapFile.getName().replace(".jnm", "") : Lang.getNoHTML("map.newmap");
    }

    public String getFilePath() {
        return (mapFile != null) ? mapFile.getAbsolutePath() : Lang.getNoHTML("map.unsaved");
    }

    public Layout<Device, Connection> getGraphLayout() {
        return layout;
    }

    /**
     * @return the history
     */
    public CommandHistory getHistory() {
        if (history == null) history = new CommandHistory();
        return history;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((graph == null) ? 0 : graph.hashCode());
        result = prime * result + ((mapFile == null) ? 0 : mapFile.hashCode());
        return result;
    }

    public boolean save() {
        XStream xs = XStreamHelper.getXStream();
        try {
            cleanupDevicesHistory(getVertices());
            cleanupDevicesHistory(layout.getGraph().getVertices());
            if (!(layout instanceof StaticLayout) && Settings.getBoolean("save.statify.layout", true)) { // keeps devices from being layouted anew when you open the map
                Layout<Device, Connection> statify = new StaticLayout<>(this);
                statify.setInitializer(layout);
                statify.setSize(layout.getSize());
                setLayout(statify);
            }
            String xml = xs.toXML(this);
            if (password != null && !password.isEmpty()) {
                xml = Crypto.encrypt(xml, password);
            }
            if (IO.copy(new ByteArrayInputStream(xml.getBytes("UTF-8")), new FileOutputStream(mapFile))) {
                setSaved(true);
                notifyListeners(new MapEvent(this, Type.SAVED, null));
                Logger.info("Saved " + mapFile.getAbsolutePath());
                return true;
            }
        } catch (Exception e) {
            Logger.error("Unable to save map to " + mapFile, e);
        }
        return false;
    }

    private void cleanupDevicesHistory(Collection<Device> devices) {
        for (Device d : devices) {
            cleanupHistory(d.getStatusHistory());
            for (NetworkIF nif : d.getInterfaces()) {
                if (nif instanceof PhysicalIF) cleanupHistory(nif.getStatusHistory());
            }
        }
    }

    private void cleanupHistory(LinkedList<Tuple<Date, Status>> cleanup) {
        // cleanup unhelpful "unknown" status entries
        cleanup.removeIf(tuple -> Status.UNKNOWN.equals(tuple.getSecond()));
        int maxSize = Settings.getInt("device.history.maxsize", 20);
        while (cleanup.size() > maxSize) cleanup.removeFirst();
    }

    public boolean setFile(File f) {
        if (f != null && f.exists() && f.canWrite()) {
            mapFile = f.getAbsoluteFile();
            return true;
        } else {
            return false;
        }
    }

    public void setLayout(Layout<Device, Connection> layout) {
        if (layout != null) this.layout = layout;
    }

    // Delegate-Methods

    /**
     * @param connection
     * @param devices
     * @param edgeType
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#addEdge(java.lang.Object, java.util.Collection, edu.uci.ics.jung.graph.util.EdgeType)
     */
    @Override
    public boolean addEdge(final Connection connection, final Collection<? extends Device> devices, final EdgeType edgeType) {
        return addEdge(connection, devices); //ignore EdgeType, is always UNDIRECTED
    }

    /**
     * @param connection
     * @param devices
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#addEdge(java.lang.Object, java.util.Collection)
     */
    @Override
    public boolean addEdge(final Connection connection, final Collection<? extends Device> devices) {
        return (Boolean) getHistory().execute(new Command() {
            @Override
            public Object undo() {
                boolean b = graph.removeEdge(connection);
                notifyListeners(new MapEvent(_this, Type.EDGE_REMOVED, connection));
                return b;
            }

            @Override
            public Object redo() {
                boolean b = graph.addEdge(connection, devices);
                notifyListeners(new MapEvent(_this, Type.EDGE_ADDED, connection));
                return b;
            }

            @Override
            public String toString() {
                return Lang.getNoHTML("command.add.connection")+": "+
                        String.join(" ↔ ", devices.stream().map(o -> o.getName()).collect(Collectors.toList()));
            }
        });
    }

    /**
     * @param connection
     * @param device1
     * @param device2
     * @param edgeType
     * @return
     * @see edu.uci.ics.jung.graph.Graph#addEdge(java.lang.Object, java.lang.Object, java.lang.Object, edu.uci.ics.jung.graph.util.EdgeType)
     */
    @Override
    public boolean addEdge(final Connection connection, final Device device1, final Device device2, EdgeType edgeType) {
        return addEdge(connection, device1, device2); // ignore EdgeType, is always UNDIRECTED
    }

    /**
     * @param connection
     * @param device1
     * @param device2
     * @return
     * @see edu.uci.ics.jung.graph.Graph#addEdge(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean addEdge(final Connection connection, final Device device1, final Device device2) {
        return (Boolean) getHistory().execute(new Command() {
            @Override
            public Object undo() {
                device1.removeInterface(device1.getInterfaceFor(connection));
                device2.removeInterface(device2.getInterfaceFor(connection));
                boolean b = graph.removeEdge(connection);
                notifyListeners(new MapEvent(_this, Type.EDGE_REMOVED, connection));
                return b;
            }

            @Override
            public Object redo() {
                addInterfaceIfMissing(connection, device1, device2);
                addInterfaceIfMissing(connection, device2, device1);
                boolean b = graph.addEdge(connection, device1, device2);
                notifyListeners(new MapEvent(_this, Type.EDGE_ADDED, connection));
                return b;
            }

            @Override
            public String toString() {
                return Lang.getNoHTML("command.add.connection")+": "+device1.getName()+" ↔ "+device2.getName();
            }
        });
    }

    private void addInterfaceIfMissing(Connection conection, Device device1, Device device2) {
        if (device1.getInterfaceFor(conection) == null) {
            if (device1.equals(device2)) {
                device1.addInterface(new LogicalIF(device1, conection, ""));
            } else {
                String t = device1.getType().toString() + "";
                NetworkIF nif;
                if (t.contains("Switch") || t.contains("Hub") || t.contains("Wireless")) {
                    nif = new TransparentIF(device1, conection, device2.getInterfaceFor(conection));
                } else {
                    nif = new PhysicalIF(device1, conection, "");
                }
                device1.addInterface(nif);
                NetworkIF counterpart = device2.getInterfaceFor(conection);
                if (counterpart != null && counterpart instanceof TransparentIF) {
                    ((TransparentIF) counterpart).setCounterpart(nif);
                }
            }
        }
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#addVertex(java.lang.Object)
     */
    @Override
    public boolean addVertex(final Device device) {
        return (Boolean) getHistory().execute(new Command() {
            @Override
            public Object undo() {
                boolean b = graph.removeVertex(device);
                notifyListeners(new MapEvent(_this, Type.VERTEX_REMOVED, device));
                device.removeDeviceListener(_this);
                return b;
            }

            @Override
            public Object redo() {
                boolean b = graph.addVertex(device);
                notifyListeners(new MapEvent(_this, Type.VERTEX_ADDED, device));
                device.addDeviceListener(_this);
                return b;
            }

            @Override
            public String toString() {
                return Lang.getNoHTML("command.add.device")+": "+device.getName();
            }
        });
    }

    /**
     * @param connection
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#containsEdge(java.lang.Object)
     */
    @Override
    public boolean containsEdge(Connection connection) {
        return graph.containsEdge(connection);
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#containsVertex(java.lang.Object)
     */
    @Override
    public boolean containsVertex(Device device) {
        return graph.containsVertex(device);
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#degree(java.lang.Object)
     */
    @Override
    public int degree(Device device) {
        return graph.degree(device);
    }

    /**
     * @param device1
     * @param device2
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#findEdge(java.lang.Object, java.lang.Object)
     */
    @Override
    public Connection findEdge(Device device1, Device device2) {
        return graph.findEdge(device1, device2);
    }

    /**
     * @param device1
     * @param device2
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#findEdgeSet(java.lang.Object, java.lang.Object)
     */
    @Override
    public Collection<Connection> findEdgeSet(Device device1, Device device2) {
        return graph.findEdgeSet(device1, device2);
    }

    /**
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getDefaultEdgeType()
     */
    @Override
    public EdgeType getDefaultEdgeType() {
        return graph.getDefaultEdgeType();
    }

    /**
     * @param connection
     * @return
     * @see edu.uci.ics.jung.graph.Graph#getDest(java.lang.Object)
     */
    @Override
    public Device getDest(Connection connection) {
        return graph.getDest(connection);
    }

    /**
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getEdgeCount()
     */
    @Override
    public int getEdgeCount() {
        return graph.getEdgeCount();
    }

    /**
     * @param edgeType
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getEdgeCount(edu.uci.ics.jung.graph.util.EdgeType)
     */
    @Override
    public int getEdgeCount(EdgeType edgeType) {
        return graph.getEdgeCount(edgeType);
    }

    /**
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getEdges()
     */
    @Override
    public Collection<Connection> getEdges() {
        return graph.getEdges();
    }

    /**
     * @param edgeType
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getEdges(edu.uci.ics.jung.graph.util.EdgeType)
     */
    @Override
    public Collection<Connection> getEdges(EdgeType edgeType) {
        return graph.getEdges(edgeType);
    }

    /**
     * @param connection
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getEdgeType(java.lang.Object)
     */
    @Override
    public EdgeType getEdgeType(Connection connection) {
        return graph.getEdgeType(connection);
    }

    /**
     * @param connection
     * @return
     * @see edu.uci.ics.jung.graph.Graph#getEndpoints(java.lang.Object)
     */
    @Override
    public Pair<Device> getEndpoints(Connection connection) {
        return graph.getEndpoints(connection);
    }

    /**
     * @param connection
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getIncidentCount(java.lang.Object)
     */
    @Override
    public int getIncidentCount(Connection connection) {
        return graph.getIncidentCount(connection);
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getIncidentEdges(java.lang.Object)
     */
    @Override
    public Collection<Connection> getIncidentEdges(Device device) {
        return graph.getIncidentEdges(device);
    }

    /**
     * @param connection
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getIncidentVertices(java.lang.Object)
     */
    @Override
    public Collection<Device> getIncidentVertices(Connection connection) {
        return graph.getIncidentVertices(connection);
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Graph#getInEdges(java.lang.Object)
     */
    @Override
    public Collection<Connection> getInEdges(Device device) {
        return graph.getInEdges(device);
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getNeighborCount(java.lang.Object)
     */
    @Override
    public int getNeighborCount(Device device) {
        return graph.getNeighborCount(device);
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getNeighbors(java.lang.Object)
     */
    @Override
    public Collection<Device> getNeighbors(Device device) {
        return graph.getNeighbors(device);
    }

    /**
     * @param device
     * @param connection
     * @return
     * @see edu.uci.ics.jung.graph.Graph#getOpposite(java.lang.Object, java.lang.Object)
     */
    @Override
    public Device getOpposite(Device device, Connection connection) {
        return graph.getOpposite(device, connection);
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Graph#getOutEdges(java.lang.Object)
     */
    @Override
    public Collection<Connection> getOutEdges(Device device) {
        return graph.getOutEdges(device);
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Graph#getPredecessorCount(java.lang.Object)
     */
    @Override
    public int getPredecessorCount(Device device) {
        return graph.getPredecessorCount(device);
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Graph#getPredecessors(java.lang.Object)
     */
    @Override
    public Collection<Device> getPredecessors(Device device) {
        return graph.getPredecessors(device);
    }

    /**
     * @param connection
     * @return
     * @see edu.uci.ics.jung.graph.Graph#getSource(java.lang.Object)
     */
    @Override
    public Device getSource(Connection connection) {
        return graph.getSource(connection);
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Graph#getSuccessorCount(java.lang.Object)
     */
    @Override
    public int getSuccessorCount(Device device) {
        return graph.getSuccessorCount(device);
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Graph#getSuccessors(java.lang.Object)
     */
    @Override
    public Collection<Device> getSuccessors(Device device) {
        return graph.getSuccessors(device);
    }

    /**
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getVertexCount()
     */
    @Override
    public int getVertexCount() {
        return graph.getVertexCount();
    }

    /**
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#getVertices()
     */
    @Override
    public Collection<Device> getVertices() {
        return graph.getVertices();
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Graph#inDegree(java.lang.Object)
     */
    @Override
    public int inDegree(Device device) {
        return graph.inDegree(device);
    }

    /**
     * @param device
     * @param connection
     * @return
     * @see edu.uci.ics.jung.graph.Graph#isDest(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean isDest(Device device, Connection connection) {
        return graph.isDest(device, connection);
    }

    /**
     * @param device
     * @param connection
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#isIncident(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean isIncident(Device device, Connection connection) {
        return graph.isIncident(device, connection);
    }

    /**
     * @param device1
     * @param device2
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#isNeighbor(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean isNeighbor(Device device1, Device device2) {
        return graph.isNeighbor(device1, device2);
    }

    /**
     * @param device1
     * @param device2
     * @return
     * @see edu.uci.ics.jung.graph.Graph#isPredecessor(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean isPredecessor(Device device1, Device device2) {
        return graph.isPredecessor(device1, device2);
    }

    /**
     * @param device
     * @param connection
     * @return
     * @see edu.uci.ics.jung.graph.Graph#isSource(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean isSource(Device device, Connection connection) {
        return graph.isSource(device, connection);
    }

    /**
     * @param device1
     * @param device2
     * @return
     * @see edu.uci.ics.jung.graph.Graph#isSuccessor(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean isSuccessor(Device device1, Device device2) {
        return graph.isSuccessor(device1, device2);
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Graph#outDegree(java.lang.Object)
     */
    @Override
    public int outDegree(Device device) {
        return graph.outDegree(device);
    }

    /**
     * @param connection
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#removeEdge(java.lang.Object)
     */
    @Override
    public boolean removeEdge(final Connection connection) {
        return (Boolean) getHistory().execute(new Command() {
            private Pair<Device> pair = getEndpoints(connection);

            @Override
            public Object undo() {
                if (pair.getFirst() != null) pair.getFirst().addInterface(pair.getFirst().getInterfaceFor(connection));
                if (pair.getSecond() != null)
                    pair.getSecond().addInterface(pair.getSecond().getInterfaceFor(connection));
                boolean b = graph.addEdge(connection, pair);
                notifyListeners(new MapEvent(_this, Type.EDGE_ADDED, connection));
                return b;
            }

            @Override
            public Object redo() {
                if (pair.getFirst() != null)
                    pair.getFirst().removeInterface(pair.getFirst().getInterfaceFor(connection));
                if (pair.getSecond() != null)
                    pair.getSecond().removeInterface(pair.getSecond().getInterfaceFor(connection));
                boolean b = graph.removeEdge(connection);
                notifyListeners(new MapEvent(_this, Type.EDGE_REMOVED, connection));
                refreshView(null);
                return b;
            }

            @Override
            public String toString() {
                return Lang.getNoHTML("command.remove.connection")+": "+pair.getFirst().getName()+" ↔ "+pair.getSecond().getName();
            }
        });
    }

    /**
     * @param device
     * @return
     * @see edu.uci.ics.jung.graph.Hypergraph#removeVertex(java.lang.Object)
     */
    @Override
    public boolean removeVertex(final Device device) {
        return (Boolean) getHistory().execute(new Command() {
            HashMap<Pair<Device>, Connection> connectors = new HashMap<>();
            HashMap<Connection, NetworkIF> oppositeIFs = new HashMap<>();

            @Override
            public Object undo() {
                boolean b = graph.addVertex(device);
                for (Pair<Device> p : connectors.keySet()) {
                    Connection c = connectors.get(p);
                    graph.addEdge(c, p);
                    Device other = (p.getFirst().equals(device)) ? p.getSecond() : p.getFirst();
                    other.addInterface(oppositeIFs.get(c));
                }
                notifyListeners(new MapEvent(_this, Type.VERTEX_ADDED, device));
                device.addDeviceListener(_this);
                return b;
            }

            @Override
            public Object redo() {
                for (Connection c : graph.getOutEdges(device)) {
                    Pair<Device> pair = graph.getEndpoints(c);
                    connectors.put(pair, c);
                    Device other = (pair.getFirst().equals(device)) ? pair.getSecond() : pair.getFirst();
                    NetworkIF nif = other.getInterfaceFor(c);
                    oppositeIFs.put(c, nif);
                    other.removeInterface(nif);
                }
                boolean b = graph.removeVertex(device);
                notifyListeners(new MapEvent(_this, Type.VERTEX_REMOVED, device));
                device.removeDeviceListener(_this);
                return b;
            }

            @Override
            public String toString() {
                return Lang.getNoHTML("command.remove.device")+": "+device.getName();
            }
        });
    }

    @Override
    public void deviceChanged(DeviceEvent e) {
        notifyListeners(new MapEvent(this, Type.DEVICE_EVENT, e));
    }

    public void setPassword(String password) {
        if (password == null) return;
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setUpdateInterval(double updateInterval) {
        this.updateInterval = updateInterval;
    }

    public double getUpdateInterval() {
        return updateInterval;
    }

    public File getBackground() {
        return background;
    }

    public void setBackground(File background) {
        this.background = background;
        try {
            if (background != null) {
                backgroundImage = new ImageIcon(background.getAbsolutePath()).getImage();
            }
        } catch (Exception e) {
            Logger.warn("Failed to load background image", e);
            this.background = null;
        }
    }

    public Image getBackgroundImage() {
        if (backgroundImage == null && background != null) setBackground(background);
        return backgroundImage;
    }

    public Set<Layer> getLayers() {
        return layers;
    }

    public void addLayer(String name) {
        layers.add(new Layer(name));
    }

    public void removeLayer(Layer layer) {
        if (layers.size() > 1) {
            Set<Device> devices = layer.getDevices();
            layers.remove(layer);
            layers.iterator().next().addDevices(devices);
        }
    }

    public Predicate<Context<Graph<Device, Connection>, Device>> getDevicePredicate() {
        return context -> {
            if (layers != null && !layers.isEmpty()) {
                for (Layer layer : layers) {
                    if (layer.containsDevice(context.element)) {
                        return layer.isVisible();
                    }
                }
            }
            return true;
        };
    }

    public void refreshView() {
        refreshView(null);
    }

    public void refreshView(Type type) {
        if (type == null) type = Type.REFRESH;
        notifyListeners(new MapEvent(this, type, null));
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public boolean isSaved() {
        return saved;
    }
}


