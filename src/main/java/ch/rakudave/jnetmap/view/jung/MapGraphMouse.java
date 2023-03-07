package ch.rakudave.jnetmap.view.jung;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.device.Device;
import edu.uci.ics.jung.visualization.MultiLayerTransformer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.annotations.AnnotatingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.*;
import org.apache.commons.collections15.Factory;

import javax.swing.*;
import javax.swing.plaf.basic.BasicIconFactory;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @author JUNG
 * @author rakudave
 */
public class MapGraphMouse extends AbstractModalGraphMouse implements ModalGraphMouse, ItemSelectable {
    private Frame owner;
    protected Factory<Device> vertexFactory;
    protected Factory<Connection> edgeFactory;
    protected EditGraphMousePlugin editingPlugin;
    protected PopupGraphMousePlugin popupEditingPlugin;
    protected AnnotatingGraphMousePlugin<Device, Connection> annotatingPlugin;
    protected MultiLayerTransformer basicTransformer;
    protected RenderContext<Device, Connection> rc;
    protected ViewScalingControl scaler;

    /**
     * create an instance with default values
     *
     * @param scaler
     */
    public MapGraphMouse(Frame owner, RenderContext<Device, Connection> rc,
                         ViewScalingControl scaler, Factory<Device> vertexFactory, Factory<Connection> edgeFactory) {
        this(owner, rc, scaler, vertexFactory, edgeFactory, 1 / 1.1f, 1.1f);
    }

    /**
     * create an instance with passed values
     *
     * @param in  override value for scale in
     * @param out override value for scale out
     */
    public MapGraphMouse(Frame owner, RenderContext<Device, Connection> rc, ViewScalingControl scaler,
                         Factory<Device> vertexFactory, Factory<Connection> edgeFactory, float in, float out) {
        super(in, out);
        this.owner = owner;
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.rc = rc;
        this.scaler = scaler;
        basicTransformer = rc.getMultiLayerTransformer();
        loadPlugins();
        setModeKeyListener(new ModeKeyAdapter(this));
    }

    /**
     * create the plugins, and load the plugins for TRANSFORMING mode
     */
    @Override
    protected void loadPlugins() {
        pickingPlugin = new PickingGraphMousePlugin<Device, Connection>();
        animatedPickingPlugin = new AnimatedPickingGraphMousePlugin<Device, Connection>();
        translatingPlugin = new TranslatingGraphMousePlugin(InputEvent.BUTTON1_MASK);
        scalingPlugin = new ScalingGraphMousePlugin(scaler, 0, in, out);
        rotatingPlugin = new RotatingGraphMousePlugin();
        shearingPlugin = new ShearingGraphMousePlugin();
        editingPlugin = new EditGraphMousePlugin(owner, vertexFactory, edgeFactory);
        annotatingPlugin = new AnnotatingGraphMousePlugin<>(rc);
        popupEditingPlugin = new PopupGraphMousePlugin(owner, vertexFactory);
        add(scalingPlugin); // zoom and middle-drag are always on
        add(new TranslatingGraphMousePlugin(InputEvent.BUTTON2_MASK));
        setMode(Mode.EDITING);
    }

    /**
     * setter for the Mode.
     */
    @Override
    public void setMode(Mode mode) {
        if (this.mode != mode) {
            fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED,
                    this.mode, ItemEvent.DESELECTED));
            this.mode = mode;
            if (mode == Mode.TRANSFORMING) {
                setTransformingMode();
            } else if (mode == Mode.PICKING) {
                setPickingMode();
            } else if (mode == Mode.EDITING) {
                setEditingMode();
            } else if (mode == Mode.ANNOTATING) {
                setAnnotatingMode();
            }
            if (modeBox != null) {
                modeBox.setSelectedItem(mode);
            }
            fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, mode, ItemEvent.SELECTED));
        }
    }

    @Override
    protected void setPickingMode() {
        remove(translatingPlugin);
        remove(rotatingPlugin);
        remove(shearingPlugin);
        remove(editingPlugin);
        remove(annotatingPlugin);
        add(pickingPlugin);
        add(animatedPickingPlugin);
        add(popupEditingPlugin);
    }

    @Override
    protected void setTransformingMode() {
        remove(pickingPlugin);
        remove(animatedPickingPlugin);
        remove(editingPlugin);
        remove(annotatingPlugin);
        add(translatingPlugin);
        add(rotatingPlugin);
        add(shearingPlugin);
        add(popupEditingPlugin);
    }

    protected void setEditingMode() {
        remove(pickingPlugin);
        remove(animatedPickingPlugin);
        remove(translatingPlugin);
        remove(rotatingPlugin);
        remove(shearingPlugin);
        remove(annotatingPlugin);
        add(editingPlugin);
        add(popupEditingPlugin);
    }

    protected void setAnnotatingMode() {
        remove(pickingPlugin);
        remove(animatedPickingPlugin);
        remove(translatingPlugin);
        remove(rotatingPlugin);
        remove(shearingPlugin);
        remove(editingPlugin);
        remove(popupEditingPlugin);
        add(annotatingPlugin);
    }


    /**
     * @return the modeBox.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public JComboBox getModeComboBox() {
        if (modeBox == null) {
            modeBox = new JComboBox(new Mode[]{Mode.TRANSFORMING, Mode.PICKING, Mode.EDITING, Mode.ANNOTATING});
            modeBox.addItemListener(getModeListener());
        }
        modeBox.setSelectedItem(mode);
        return modeBox;
    }

    /**
     * create (if necessary) and return a menu that will change
     * the mode
     *
     * @return the menu
     */
    @Override
    public JMenu getModeMenu() {
        if (modeMenu == null) {
            modeMenu = new JMenu();// {
            Icon icon = BasicIconFactory.getMenuArrowIcon();
            modeMenu.setIcon(BasicIconFactory.getMenuArrowIcon());
            modeMenu.setPreferredSize(new Dimension(icon.getIconWidth() + 10,
                    icon.getIconHeight() + 10));

            final JRadioButtonMenuItem transformingButton =
                    new JRadioButtonMenuItem(Mode.TRANSFORMING.toString());
            transformingButton.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setMode(Mode.TRANSFORMING);
                }
            });

            final JRadioButtonMenuItem pickingButton =
                    new JRadioButtonMenuItem(Mode.PICKING.toString());
            pickingButton.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setMode(Mode.PICKING);
                }
            });

            final JRadioButtonMenuItem editingButton =
                    new JRadioButtonMenuItem(Mode.EDITING.toString());
            editingButton.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setMode(Mode.EDITING);
                }
            });

            ButtonGroup radio = new ButtonGroup();
            radio.add(transformingButton);
            radio.add(pickingButton);
            radio.add(editingButton);
            transformingButton.setSelected(true);
            modeMenu.add(transformingButton);
            modeMenu.add(pickingButton);
            modeMenu.add(editingButton);
            modeMenu.setToolTipText("Menu for setting Mouse Mode");
            addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (e.getItem() == Mode.TRANSFORMING) {
                        transformingButton.setSelected(true);
                    } else if (e.getItem() == Mode.PICKING) {
                        pickingButton.setSelected(true);
                    } else if (e.getItem() == Mode.EDITING) {
                        editingButton.setSelected(true);
                    }
                }
            });
        }
        return modeMenu;
    }

    public static class ModeKeyAdapter extends KeyAdapter {
        private char t = 't';
        private char p = 'p';
        private char e = 'e';
        private char a = 'a';
        protected ModalGraphMouse graphMouse;

        public ModeKeyAdapter(ModalGraphMouse graphMouse) {
            this.graphMouse = graphMouse;
        }

        public ModeKeyAdapter(char t, char p, char e, char a, ModalGraphMouse graphMouse) {
            this.t = t;
            this.p = p;
            this.e = e;
            this.a = a;
            this.graphMouse = graphMouse;
        }

        @Override
        public void keyTyped(KeyEvent event) {
            char keyChar = event.getKeyChar();
            if (keyChar == t) {
                ((Component) event.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                graphMouse.setMode(Mode.TRANSFORMING);
            } else if (keyChar == p) {
                ((Component) event.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                graphMouse.setMode(Mode.PICKING);
            } else if (keyChar == e) {
                ((Component) event.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                graphMouse.setMode(Mode.EDITING);
            } else if (keyChar == a) {
                ((Component) event.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                graphMouse.setMode(Mode.ANNOTATING);
            }
        }
    }

    /**
     * @return the annotatingPlugin
     */
    public AnnotatingGraphMousePlugin<Device, Connection> getAnnotatingPlugin() {
        return annotatingPlugin;
    }

    /**
     * @return the editingPlugin
     */
    public EditGraphMousePlugin getEditingPlugin() {
        return editingPlugin;
    }

    /**
     * @return the popupEditingPlugin
     */
    public PopupGraphMousePlugin getPopupEditingPlugin() {
        return popupEditingPlugin;
    }
}
