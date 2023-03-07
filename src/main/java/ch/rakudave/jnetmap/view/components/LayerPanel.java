package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.model.CurrentMapListener;
import ch.rakudave.jnetmap.model.Layer;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;


@SuppressWarnings("serial")
public class LayerPanel extends JPanel implements CurrentMapListener {
    private DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
    private JButton add, remove;
    private DefaultTreeModel treeModel;

    public LayerPanel(final Frame owner) {
        Controller.addCurrentMapListener(this);
        setLayout(new BorderLayout());
        JTree tree = new JTree(rootNode);
        treeModel = (DefaultTreeModel) tree.getModel();
        tree.setRootVisible(false);
        tree.setAutoscrolls(true);
        tree.setDragEnabled(true);
        tree.setTransferHandler(new LayerTransferHandler());
        tree.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.setCellRenderer(new LayerRenderer());

        JPanel bottom = new JPanel(new GridLayout(1, 2));
        putAddButton(bottom);
        putRemoveButton(bottom);

        add(new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void putAddButton(JPanel panel) {
        add = new JButton(new AbstractAction(Lang.get("action.add"), Icons.get("add")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                rootNode.add(new DefaultMutableTreeNode(new Layer("new"), true));
                treeModel.reload();
            }
        });
        panel.add(add);
    }

    private void putRemoveButton(JPanel panel) {
        remove = new JButton(new AbstractAction(Lang.get("action.delete"), Icons.get("remove")) {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        panel.add(remove);
    }

    @Override
    public void mapChanged(Map map) {
        rootNode.removeAllChildren();
        DefaultMutableTreeNode def = new DefaultMutableTreeNode(new Layer("new"), true);
        rootNode.add(def);
        for (Device device : map.getVertices()) {
            def.add(new DefaultMutableTreeNode(device, false));
        }
        treeModel.reload();
    }

    private class LayerRenderer extends JLabel implements TreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean isSelected, boolean isExpanded, boolean isLeaf, int row, boolean hasFocus) {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject instanceof Device) {
                Device device = (Device) userObject;
                setText(device.getName());
                setIcon(Icons.get("new"));
            } else if (userObject instanceof Layer) {
                Layer layer = (Layer) userObject;
                return layer.getComponent();
            }
            setPreferredSize(new Dimension(999, 24));
            return this;
        }
    }

    private class LayerTransferHandler extends TransferHandler {

    }
}
