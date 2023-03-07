package ch.rakudave.jnetmap.util;

import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.IF.PhysicalIF;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.components.InterfaceSelector;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * @author rakudave
 */
public class SwingHelper {

    /**
     * Center a window relative to a parent window
     *
     * @param parent
     * @param child
     */
    public static void centerTo(Window parent, Window child) {
        if (parent == null || child == null) return;
        Point parentLocation = parent.getLocationOnScreen();
        child.setLocation(parentLocation.x + (parent.getWidth() / 2) - (child.getWidth() / 2),
                parentLocation.y + (parent.getHeight() / 2) - (child.getHeight() / 2));
    }

    /**
     * Shortcut, creates leading-aligned button
     */
    public static JButton createAlignedButton(Action a) {
        JButton b = new JButton(a);
        b.setHorizontalAlignment(JButton.LEADING);
        return b;
    }

    /**
     * Prompt to select a file
     *
     * @param owner  parent frame
     * @param filter filetype
     * @return selected file or null if canceled
     */
    public static File openDialog(Component owner, FileNameExtensionFilter filter) {
        Object[] o = openDialog(owner, filter, false);
        return (o == null) ? null : (File) o[0];
    }

    /**
     * Prompt to select a file
     *
     * @param owner    parent frame
     * @param filter   filetype
     * @param withPass show a password field
     * @return null if canceled, [selected file, password] otherwise
     */
    public static Object[] openDialog(Component owner, FileNameExtensionFilter filter, boolean withPass) {
        JFileChooser chooser = new JFileChooser();
        if (filter != null) chooser.setFileFilter(filter);
        Object[] o = getAccessory("");
        if (withPass) chooser.setAccessory((JComponent) o[0]);
        if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) return null;
        File f = chooser.getSelectedFile();
        if (!f.canRead()) return null;
        return new Object[]{f, String.valueOf(((JPasswordField) o[1]).getPassword())};
    }

    /**
     * Prompt to save a file
     *
     * @param owner  parent frame
     * @param filter filetype
     * @return selected file or null if canceled
     */
    public static File saveDialog(Component owner, FileNameExtensionFilter filter) {
        Object[] o = saveDialog(owner, filter, false);
        return (o == null) ? null : (File) o[0];
    }

    /**
     * Prompt to save a file
     *
     * @param owner    parent frame
     * @param filter   filetype
     * @param withPass show a password field
     * @return null if canceled, [selected file, password] otherwise
     */
    public static Object[] saveDialog(Component owner, FileNameExtensionFilter filter, boolean withPass) {
        JFileChooser chooser = new JFileChooser();
        if (filter != null) chooser.setFileFilter(filter);
        Object[] o = getAccessory("");
        if (withPass) chooser.setAccessory((JComponent) o[0]);
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return null;
        String path = chooser.getSelectedFile().getPath();
        if (filter != null && filter.getExtensions().length > 0 && !path.matches("(.*)." + filter.getExtensions()[0]))
            path += "." + filter.getExtensions()[0];
        File f = new File(path);
        if (!f.exists())
            try {
                f.createNewFile();
            } catch (IOException e) {
                Logger.error("Failed to create file at " + f.getAbsolutePath(), e);
                return null;
            }
        return new Object[]{f, String.valueOf(((JPasswordField) o[1]).getPassword())};
    }

    public static String interfaceSelector(Device d) {
        return interfaceSelector(d, false);
    }

    public static String interfaceSelector(Device d, boolean byName) {
        List<NetworkIF> ifs = d.getInterfaces();
        if (ifs.size() < 1) return null;
        Vector<String> addresses = new Vector<>();
        for (NetworkIF nif : ifs) {
            if (nif instanceof PhysicalIF) {
                addresses.add((byName) ? nif.getAddress().getHostName() : nif.getAddress().getHostAddress());
            }
        }
        if (addresses.isEmpty()) {
            return null;
        } else if (addresses.size() == 1) {
            return addresses.get(0);
        } else {
            InterfaceSelector is = new InterfaceSelector((Frame) Controller.getView(), d, addresses);
            return is.getSelected();
        }
    }

    private static Object[] getAccessory(String password) {
        JPanel p = new JPanel(new BorderLayout());
        p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
        final JPasswordField pass = new JPasswordField(password);
        pass.setEnabled(false);
        pass.setMaximumSize(new Dimension(200, 30));
        final JCheckBox useCrypto = new JCheckBox(Lang.get("action.password"));
        useCrypto.addActionListener(e -> {
            pass.setEnabled(useCrypto.isSelected());
            if (!useCrypto.isSelected()) pass.setText("");
        });
        p.add(Box.createVerticalGlue());
        p.add(useCrypto);
        p.add(pass);
        p.add(Box.createVerticalGlue());
        return new Object[]{p, pass};
    }

    public static String passwordPrompt() {
        JPasswordField pwd = new JPasswordField(25);
        int action = JOptionPane.showConfirmDialog(null, pwd, Lang.getNoHTML("action.password") + ":", JOptionPane.OK_CANCEL_OPTION);
        if (action < 0) return null;
        else return new String(pwd.getPassword());
    }

    public static JEditorPane createHtmlLabel(String text) {
        JEditorPane ep = new JEditorPane();
        ep.setContentType("text/html");
        ep.setText(text);
        ep.setBackground(new Color(0, 0, 0, 0));
        ep.setEditable(false);
        ep.setFocusable(false);
        return ep;
    }

    public static FontUIResource getViewFont() {
        return new FontUIResource(
                Settings.get("view.font.name", Font.SANS_SERIF), Font.PLAIN,
                Settings.getInt("view.font.size", 12));
    }

    public static String getViewFontAsCss() {
        return "font-family: "+Settings.get("view.font.name", Font.SANS_SERIF)+"; "
                +"font-size: "+Settings.getInt("view.font.size", 12)+";";
    }

    public static void setUIFont(FontUIResource font) {
        if (font == null) font = getViewFont();
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, font);
            }
        }
    }

    private SwingHelper() {
    }
}
