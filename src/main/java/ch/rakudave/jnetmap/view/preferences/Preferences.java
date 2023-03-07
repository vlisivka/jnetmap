package ch.rakudave.jnetmap.view.preferences;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.plugins.JNetMapPlugin;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.SwingHelper;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.IView;
import ch.rakudave.jnetmap.view.components.EscapableDialog;
import ch.rakudave.jnetmap.view.components.TabPanel;
import org.pf4j.PluginWrapper;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.Enumeration;

/**
 * @author rakudave
 */
public class Preferences extends EscapableDialog {
    private static final long serialVersionUID = 6209354869982252918L;
    private DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
    private JPanel container;

    private IView parent;

    public Preferences(IView parent) {
        super((Window) parent, Lang.getNoHTML("menu.edit.preferences"));
        setMinimumSize(new Dimension(550, 660));
        setPreferredSize(new Dimension(550, 660));
        setLayout(new BorderLayout());
        this.parent = parent;
        final CardLayout cl = new CardLayout();
        container = new JPanel(cl);
        if (rootNode.getChildCount() == 0) {
            addCard(new GeneralPanel((Window) parent));
            addCard(new AppearancePanel((Window) parent, this));
            addCard(new ToolBarPanel());
            addCard(new PluginPanel());
            for (PluginWrapper wrapper : Controller.getPluginManager().getPlugins()) {
                if (wrapper.getPlugin() instanceof JNetMapPlugin) {
                    JNetMapPlugin plugin = (JNetMapPlugin) wrapper.getPlugin();
                    if (plugin.hasSettings()) {
                        PreferencePanel panel = plugin.getSettingsPanel();
                        panel.title = plugin.getPluginName();
                        panel.parentTitle = Lang.getNoHTML("preferences.plugins");
                        addCard(panel);
                    }
                }
            }
            addCard(new ScriptsPanel());
            addCard(new LoggingPanel());
        }
        JTree tree = new JTree(rootNode);
        tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tree.setRootVisible(false);
        tree.addTreeSelectionListener(tse -> {
            TreeCard node = (TreeCard) ((DefaultMutableTreeNode) tse
                    .getPath().getLastPathComponent()).getUserObject();
            if (node.nodePanel != null) {
                String cardLayoutID = node.ID;
                cl.show(container, cardLayoutID);
            }
        });
        JPanel bottomRow = new JPanel();
        bottomRow.setLayout(new BoxLayout(bottomRow, BoxLayout.LINE_AXIS));
        JButton cancel = new JButton(Lang.get("action.cancel"), Icons.get("cancel"));
        cancel.setMaximumSize(new Dimension(120, 30));
        cancel.setPreferredSize(new Dimension(120, 30));
        cancel.addActionListener(e -> Preferences.this.dispose());
        JButton apply = new JButton(Lang.get("action.apply"), Icons.get("right"));
        apply.setMaximumSize(new Dimension(120, 30));
        apply.setPreferredSize(cancel.getPreferredSize());
        apply.addActionListener(e -> {
            save();
            if (TabPanel.getCurrentTab() != null) TabPanel.getCurrentTab().repaint();
        });
        JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
        ok.setMaximumSize(new Dimension(120, 30));
        ok.setPreferredSize(cancel.getPreferredSize());
        ok.addActionListener(e -> {
            save();
            Preferences.this.dispose();
            if (TabPanel.getCurrentTab() != null) TabPanel.getCurrentTab().repaint();
            // TODO check if restart needed
        });
        bottomRow.add(Box.createHorizontalStrut(2));
        bottomRow.add(apply);
        bottomRow.add(Box.createHorizontalGlue());
        bottomRow.add(cancel);
        bottomRow.add(Box.createHorizontalStrut(5));
        bottomRow.add(ok);
        bottomRow.add(Box.createHorizontalStrut(2));
        add(new JScrollPane(tree), BorderLayout.WEST);
        add(container, BorderLayout.CENTER);
        add(bottomRow, BorderLayout.SOUTH);
        pack();
        setVisible(true);
    }

    public void addCard(PreferencePanel card) {
        TreeCard newCard = new TreeCard(card.getTitle(), card.getParentTitle(), card);
        MutableTreeNode mtn = new DefaultMutableTreeNode(newCard);
        if (card.getParentTitle().isEmpty()) {
            rootNode.add(mtn);
        } else {
            for (int i = 0; i < rootNode.getChildCount(); i++) {
                Enumeration<TreeNode> tree = rootNode.breadthFirstEnumeration();
                tree.nextElement(); //skip root
                while (tree.hasMoreElements()) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) tree.nextElement();
                    if (((TreeCard) child.getUserObject()).nodeName.equals(card.getParentTitle())) {
                        child.add(mtn);
                        return;
                    }
                }
            }
            Logger.error("No parent named " + card.getParentTitle() + " was found!");
        }
    }

    protected void save() {
        Enumeration<TreeNode> tree = rootNode.preorderEnumeration();
        tree.nextElement(); //skip root
        while (tree.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) tree.nextElement();
            TreeCard card = (TreeCard) child.getUserObject();
            try {
                card.nodePanel.save();
            } catch (Exception e) {
                Logger.error("Preferences of '" + card.nodeName + "' could not be saved", e);
            }
        }
        Settings.save();
    }

    @Override
    public void setVisible(boolean b) {
        SwingHelper.centerTo((Window) parent, this);
        super.setVisible(b);
    }


    private class TreeCard {
        private String nodeName;
        private String ID;
        private PreferencePanel nodePanel;

        public TreeCard(String title, String parent, PreferencePanel panel) {
            nodeName = title;
            ID = parent.isEmpty() ? nodeName : parent + " - " + nodeName;
            nodePanel = panel;
            nodePanel.setBorder(BorderFactory.createTitledBorder(ID));
            container.add(ID, nodePanel);
        }

        @Override
        public String toString() {
            return nodeName;
        }
    }
}