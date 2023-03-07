package ch.rakudave.jnetmap.view.components;

import ch.rakudave.jnetmap.controller.Actions;
import ch.rakudave.jnetmap.controller.Controller;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.SwingHelper;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.FontUIResource;
import java.awt.*;

@SuppressWarnings("serial")
public class AboutDialog extends EscapableDialog implements HyperlinkListener {

    public AboutDialog(final Frame owner) {
        super(owner, Lang.getNoHTML("about"));
        setLayout(new BorderLayout(5, 5));
        setMinimumSize(new Dimension(440, 700));
        setPreferredSize(new Dimension(440, 700));
        JLabel jNetMap = new JLabel("Version " + Controller.version +
                " (on Java " + System.getProperty("java.version")+ " from " + System.getProperty("java.vendor") + ")",
                new ImageIcon(this.getClass().getResource("/img/splash-tb.png")), JLabel.CENTER);
        jNetMap.setVerticalTextPosition(JLabel.BOTTOM);
        jNetMap.setHorizontalTextPosition(JLabel.CENTER);
        FontUIResource viewFont = SwingHelper.getViewFont();
        JEditorPane by = SwingHelper.createHtmlLabel(contributors);
        by.setFont(viewFont);
        by.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
        JEditorPane gpl = SwingHelper.createHtmlLabel(license);
        gpl.setFont(viewFont);
        gpl.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
        gpl.addHyperlinkListener(this);
        JEditorPane art = SwingHelper.createHtmlLabel(artwork);
        art.setFont(viewFont);
        art.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
        art.addHyperlinkListener(this);
        JTabbedPane tp = new JTabbedPane();
        tp.add("Credits", by);
        tp.add("License", gpl);
        tp.add("Artwork", art);
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JButton ok = new JButton(Lang.get("action.ok"), Icons.get("ok"));
        ok.addActionListener(e -> AboutDialog.this.dispose());
        bottomRow.add(ok);
        add(jNetMap, BorderLayout.NORTH);
        add(tp, BorderLayout.CENTER);
        add(bottomRow, BorderLayout.SOUTH);
        pack();
        SwingHelper.centerTo(owner, this);
        setVisible(true);
    }

    private static final String contributors = "<table width=\"100%\">"
            + "<tr><b>Development:</b></tr>"
            + "<tr><td>Lead Developer:</td><td>rakudave</td></tr>"
            + "<tr><td>Dev, Testing:</td><td>sebehuber</td></tr>"
            + "<tr></tr>"
            + "<tr><b>Translation:</b></tr>"
            + "<tr><td>Arabic:</td><td>Arick McNiel-Chov, Zeki Abdulaali</td></tr>"
            + "<tr><td></td><td>and Samuel K. Michael</td></tr>"
            + "<tr><td>English, German:</td><td>rakudave</td></tr>"
            + "<tr><td>French:</td><td>Vincent Knecht</td></tr>"
            + "<tr><td>Hungarian:</td><td>Zoltan Fekete</td></tr>"
            + "<tr><td>Portuguese:</td><td>Delton Giacomozzi</td></tr>"
            + "<tr><td>Spanish:</td><td>Lenny Qebian</td></tr>"
            + "<tr><td>Russian:</td><td>s-r-grass (КонтинентСвободы.рф)</td></tr>"
            + "</table>";

    private static final String license = "<p>This program is free software: you can redistribute it and/or modify\n" +
            "it under the terms of the GNU General Public License as published by\n" +
            "the Free Software Foundation, either version 3 of the License, or\n" +
            "(at your option) any later version.</p>" +
            "<p>This program is distributed in the hope that it will be useful,\n" +
            "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
            "GNU General Public License for more details.</p>" +
            "<p>You should have received a copy of the GNU General Public License\n" +
            "along with this program.  If not, see <a href=\"https://www.gnu.org/licenses/\">https://www.gnu.org/licenses/</a>.</p>";

    private static final String artwork = "<table width=\"100%\">"
            + "<tr><td><b>Icons</b></td><td></td></tr>"
            + "<tr><td>Elementary</td><td><a href=\"http://danrabbit.deviantart.com/art/elementary-Icons-65437279\">DanRabbit</a></td></tr>"
            + "<tr><td>Human</td><td><a href=\"http://ubuntu.com\">Ubuntu</a></td></tr>"
            + "<tr><td>Symbolize</td><td><a href=\"http://dryicons.com/free-icons/preview/symbolize-icons-set\">DryIcons</a></td></tr>"
            + "<tr><td>Tango</td><td><a href=\"http://tango.freedesktop.org/Tango_Icon_Library\">Tango Project</a></td></tr>"
            + "<tr><td></td><td></td></tr>"
            + "<tr><td><b>Devices</b></td><td></td></tr>"
            + "<tr><td>Cisco</td><td><a href=\"http://www.cisco.com/web/about/ac50/ac47/2.html\">cisco inc.</a></td></tr>"
            + "<tr><td>Human-o2</td><td><a href=\"http://schollidesign.deviantart.com/art/Human-O2-Iconset-105344123\">Oliver Scholtz</a></td></tr>"
            + "<tr><td>Reflection</td><td><a href=\"http://www.webdesignerdepot.com/2010/07/200-exclusive-free-icons-reflection\">webdesignerdepot</a></td></tr>"
            + "<tr><td></td><td></td></tr>"
            + "<tr><td><b>Themes</b></td><td></td></tr>"
            + "<tr><td>FlatLaf</td><td><a href=\"https://www.formdev.com/flatlaf\">FormDev</a></td></tr>"
            + "</table>";

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == EventType.ACTIVATED) {
            Actions.openWebsite(e.getDescription()); //don't ask me why getURL() doesn't work
        }
    }
}
