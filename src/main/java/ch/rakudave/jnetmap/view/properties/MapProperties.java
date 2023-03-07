package ch.rakudave.jnetmap.view.properties;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.controller.StatusUpdater;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.plugins.extensions.Notifier;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.components.EscapableDialog;
import ch.rakudave.jnetmap.view.components.TabPanel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

@SuppressWarnings("serial")
public class MapProperties extends EscapableDialog {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapProperties(final Frame owner, final Map m) {
        super(owner, Lang.getNoHTML("map.properties"));
        setLayout(new BorderLayout(5, 5));
        setPreferredSize(new Dimension(400, 400));
        setMinimumSize(new Dimension(400, 300));
        JPanel topWrapper = new JPanel(new GridLayout(0, 2, 5, 5));
        topWrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        final SpinnerNumberModel updateInterval = new SpinnerNumberModel(m.getUpdateInterval(), 0, 60, 0.1);
        JSpinner updateSpinner = new JSpinner(updateInterval);
        topWrapper.add(new JLabel(Lang.get("map.updateinterval")));
        topWrapper.add(updateSpinner);
        topWrapper.add(new JLabel(Lang.get("map.background")));
        BackgroundChooser bc = new BackgroundChooser(this, m.getBackground());
        topWrapper.add(bc);
        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.setBorder(BorderFactory.createTitledBorder(Lang.getNoHTML("map.notifiers")));
        final JList listenerList = new JList(new Vector<>(m.getStatusListeners()));
        listenerList.setCellRenderer(new IconListRenderer(true));
        JPanel addWrapper = new JPanel(new BorderLayout());
        final JComboBox addSelector = new JComboBox(new Vector<Notifier>() {{
            List<Notifier> notifiers = Controller.getPluginManager().getExtensions(Notifier.class);
            notifiers.sort(Comparator.comparing(Notifier::getName));
            for (Notifier notifier : notifiers) {
                try {
                    add(notifier);
                } catch (Exception e) {
                    Logger.error("Unable to load plugin " + notifier, e);
                }
            }
        }});
        addSelector.setRenderer(new IconListRenderer(false));
        JButton addListener = new JButton(Lang.get("action.add"), Icons.get("add"));
        addListener.setPreferredSize(new Dimension(100, 30));
        addListener.addActionListener(e -> {
            Notifier newListener = ((Notifier) addSelector.getSelectedItem()).create();
            m.addStatusListener(newListener);
            newListener.showPropertiesWindow(owner, true);
            listenerList.setListData(new Vector<Notifier>(m.getStatusListeners()));
        });
        addWrapper.add(addSelector, BorderLayout.CENTER);
        addWrapper.add(addListener, BorderLayout.EAST);
        JPanel listManipulators = new JPanel();
        listManipulators.setLayout(new BoxLayout(listManipulators, BoxLayout.PAGE_AXIS));
        JButton removeListener = new JButton(Lang.get("action.remove"), Icons.get("remove"));
        removeListener.setPreferredSize(new Dimension(100, 30));
        removeListener.addActionListener(e -> {
            if (listenerList.getSelectedIndex() == -1) return;
            m.removeStatusListener((Notifier) listenerList.getSelectedValue());
            listenerList.setListData(new Vector<Notifier>(m.getStatusListeners()));
        });
        JButton editListener = new JButton(Lang.get("action.edit"), Icons.get("edit"));
        editListener.setPreferredSize(new Dimension(100, 30));
        editListener.addActionListener(e -> {
            if (listenerList.getSelectedIndex() == -1) return;
            ((Notifier) listenerList.getSelectedValue()).showPropertiesWindow(owner, false);
            listenerList.setListData(new Vector<Notifier>(m.getStatusListeners()));
        });
        listManipulators.add(removeListener);
        listManipulators.add(editListener);
        //listManipulators.add(testListener);
        listManipulators.add(Box.createVerticalGlue());
        listWrapper.add(addWrapper, BorderLayout.NORTH);
        listWrapper.add(new JScrollPane(listenerList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        listWrapper.add(listManipulators, BorderLayout.EAST);
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        final JDialog _this = this;
        JButton cancel = new JButton(Lang.get("action.cancel"), Icons.get("cancel"));
        cancel.addActionListener(e -> _this.dispose());
        JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
        ok.setPreferredSize(cancel.getPreferredSize());
        ok.addActionListener(e -> {
            double newInterval = updateInterval.getNumber().doubleValue();
            if (m.getUpdateInterval() != newInterval) {
                Logger.debug("Update schedule changed from " + m.getUpdateInterval() + " to " + newInterval);
                m.setUpdateInterval(newInterval);
                StatusUpdater.updateTimeInterval(m);
            }
            m.setBackground(bc.getSelectedFile());
            m.setSaved(false);
            _this.dispose();
            if (TabPanel.getCurrentTab() != null) TabPanel.getCurrentTab().repaint();
        });
        bottomRow.add(cancel);
        bottomRow.add(ok);
        add(topWrapper, BorderLayout.NORTH);
        add(listWrapper, BorderLayout.CENTER);
        add(bottomRow, BorderLayout.SOUTH);
        pack();
        SwingHelper.centerTo(owner, this);
        setVisible(true);
    }

    private class BackgroundChooser extends JPanel {
        private JTextField tf;

        public BackgroundChooser(Component owner, File file) {
            super(new BorderLayout());
            tf = new JTextField(file == null ? "" : file.getAbsolutePath());
            add(tf, BorderLayout.CENTER);
            JButton b = new JButton("..");
            b.addActionListener(e -> {
                try {
                    tf.setText(SwingHelper.openDialog(owner, new FileNameExtensionFilter("jpg, png", "jpg", "jpeg", "png")).getAbsolutePath());
                } catch (Exception ex) {
                    Logger.debug("Invalid file received", ex);
                }
            });
            add(b, BorderLayout.EAST);
        }

        public File getSelectedFile() {
            File f = new File(tf.getText());
            return f.canRead() ? f : null;
        }
    }

    private class IconListRenderer extends DefaultListCellRenderer {
        private boolean showName;

        public IconListRenderer(boolean showName) {
            this.showName = showName;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);
            if (value == null) return label;
            Notifier l = (Notifier) value;
            label.setText((showName) ? l.getPluginName() + ": " + l.getName() : l.getPluginName());
            label.setIcon(l.getIcon());
            return label;
        }
    }
}
