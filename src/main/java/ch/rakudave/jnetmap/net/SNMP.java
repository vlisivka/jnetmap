package ch.rakudave.jnetmap.net;

import ch.rakudave.jnetmap.controller.Scheduler;
import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.IF.PhysicalIF;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.util.IO;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author techdive.in
 * @author sebehuber
 * @author rakudave
 */

public class SNMP {
    private static Properties mibs;

    public SNMP() {
    }

    /**
     * This Method returns a HashMap with the description of the SNMP OID as Key
     * and the Result from the SNMP Request as Value. The request is sent to the
     * InetAddress given by parameter. The request goes over the specified Port
     * in the Preferences. The request which get executed are stored in the file
     * MIB. Other request can easily be added manually in the MIB file.
     *
     * @return HashMap
     */
    @SuppressWarnings("unchecked")
    public static HashMap<String, String> getValues(InetAddress address) {
        HashMap<String, String> oid = new HashMap<>();
        if (address == null) return oid;
        CommunityTarget comtarget = new CommunityTarget();
        comtarget.setCommunity(new OctetString(Settings.get("snmp.community", "public")));
        comtarget.setVersion(Settings.getInt("snmp.version", 1));
        comtarget.setAddress(new UdpAddress(address.getHostAddress() + "/" + Settings.getInt("snmp.port", 161)));
        comtarget.setRetries(2);
        comtarget.setTimeout(Settings.getInt("snmp.timeout", 3000));
        TransportMapping transport;
        try {
            transport = new DefaultUdpTransportMapping();
            transport.listen();
        } catch (IOException e) {
            Logger.error("Failed to open UDP transport", e);
            return oid;
        }
        Snmp snmp = new Snmp(transport);
        PDU pdu = new PDU();
        pdu.setType(PDU.GET);

        if (mibs == null) mibs = loadMIB();
        HashMap<String, String> names = new HashMap<>();
        for (Object key : mibs.keySet()) {
            String name = key.toString();
            pdu.add(new VariableBinding(new OID(mibs.getProperty(name))));
            names.put(mibs.getProperty(name), name);
        }

        Logger.debug("SNMP " + comtarget.toString());
        ResponseEvent response;
        try {
            response = snmp.get(pdu, comtarget);
            if (response != null) {
                PDU responsePDU = response.getResponse();
                if (responsePDU != null) {
                    int errorStatus = responsePDU.getErrorStatus();
                    if (errorStatus == PDU.noError) {
                        for (VariableBinding vb : responsePDU.getVariableBindings()) {
                            String value = vb.getVariable().toString();
                            if (value != null && !"".equals(value)) oid.put(names.get(vb.getOid().toString()), value);
                        }
                        Logger.debug(oid.toString());
                    } else {
                        int errorIndex = responsePDU.getErrorIndex();
                        String errorStatusText = responsePDU.getErrorStatusText();
                        Logger.error("SNMP error: status " + errorStatus + ", index " + errorIndex + ", text " + errorStatusText);
                    }
                } else {
                    Logger.debug("SNMP timeout");
                }
            }
            snmp.close();
        } catch (IOException e) {
            Logger.debug("SNMP request failed", e);
        }

        return oid;
    }

    public static void inferProperties(final Device d) {
        Scheduler.execute(() -> {
            Map<String, String> m = null;
            for (NetworkIF nif : d.getInterfaces()) {
                m = getValues(nif.getAddress());
                if (m != null) {
                    inferProperties(nif);
                    break;
                }
            }
            if (m == null) return;
            d.setName(m.get("Name"));
            d.setDescription(m.get("Descr"));
        });
    }

    public static void inferProperties(NetworkIF nif) {
        Map<String, String> m = getValues(nif.getAddress());
        if (m == null) return;
        try {
            if (m.get("IF-speed") != null)
                nif.getConnection().setBandwidth(Double.valueOf(m.get("IF-speed")) / 1000000);
        } catch (Exception e) {
            Logger.warn("Unable to set interface speed", e);
        }
        if (m.get("MAC-address") != null && nif instanceof PhysicalIF)
            ((PhysicalIF) nif).setMacAddress(m.get("MAC-address"));
    }

    private static Properties loadMIB() {
        return IO.getMergedProps("/MIB");
    }
}
