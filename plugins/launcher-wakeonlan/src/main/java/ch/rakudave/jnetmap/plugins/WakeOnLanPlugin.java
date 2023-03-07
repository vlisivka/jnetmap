package ch.rakudave.jnetmap.plugins;

import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.IF.PhysicalIF;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.plugins.extensions.RightClickAction;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.SwingHelper;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.preferences.PreferencePanel;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

import javax.swing.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * @author rakudave
 */

public class WakeOnLanPlugin extends JNetMapPlugin {

    public WakeOnLanPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    public static class WakeOnLan implements RightClickAction {

        public WakeOnLan() {
        }

        public static byte[] getMacBytes(String macStr) throws IllegalArgumentException {
            byte[] bytes = new byte[6];
            String[] hex = macStr.split("([:-])");
            if (hex.length != 6) throw new IllegalArgumentException("Invalid MAC address.");
            try {
                for (int i = 0; i < 6; i++) {
                    bytes[i] = (byte) Integer.parseInt(hex[i], 16);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid hex digit in MAC address", e);
            }
            return bytes;
        }

        /**
         * Sends a "magic packet" to a MAC-Address in a specific subnet.
         * Provided the machine supports WakeOnLan, it should start booting.
         *
         * @param broadcastIP broadcast IP of the subnet
         * @param macAddress  Media Access Control address;
         *                    Format: aa:bb:cc:dd:ee:ff or aa-bb-cc-dd-ee-ff
         * @return package was sent
         */
        public static boolean wake(InetAddress broadcastIP, String macAddress) {
            DatagramSocket socket = null;
            try {
                byte[] macBytes = getMacBytes(macAddress);
                byte[] bytes = new byte[6 + 16 * macBytes.length];
                for (int i = 0; i < 6; i++) {
                    bytes[i] = (byte) 0xff;
                }
                for (int i = 6; i < bytes.length; i += macBytes.length) {
                    System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
                }
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, broadcastIP, 9);
                socket = new DatagramSocket();
                socket.send(packet);
                socket.close();
                return true;
            } catch (Exception e) {
                Logger.error("Failed to send Wake-on-LAN packet to " + macAddress, e);
                return false;
            } finally {
                if (socket != null) socket.close();
            }
        }

        @Override
        public void execute(Device d) {
            String ip = SwingHelper.interfaceSelector(d);
            if (ip == null) return;
            try {
                PhysicalIF nif = null;
                for (NetworkIF n : d.getInterfaces()) {
                    if (ip.equals(n.getAddress().getHostAddress()))
                        nif = (PhysicalIF) n;
                }
                if (nif == null) return;
                wake(InetAddress.getByName(nif.getSubnet().getInfo().getBroadcastAddress()), nif.getMacAddress());
            } catch (Exception e) {
                Logger.error("Unable to WakeOnLAN " + ip, e);
            }
        }

        @Override
        public Icon getIcon() {
            return Icons.get("up");
        }

        @Override
        public String getName() {
            return "Wake On LAN";
        }

    }

    @Override
    public String getAuthor() {
        return "rakudave";
    }

    @Override
    public String getDescription() {
        return "Sends a 'magic packet' to a MAC-Address, causing compatible devices to start up";
    }

    @Override
    public Icon getIcon() {
        return Icons.get("up");
    }

    @Override
    public String getPluginName() {
        return "Wake On LAN";
    }

    @Override
    public PreferencePanel getSettingsPanel() {
        return null;
    }

    @Override
    public boolean hasSettings() {
        return false;
    }
}
