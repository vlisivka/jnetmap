package ch.rakudave.jnetmap.controller;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.IF.NetworkIF;
import ch.rakudave.jnetmap.model.IF.PhysicalIF;
import ch.rakudave.jnetmap.model.Map;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.net.Arp;
import ch.rakudave.jnetmap.util.Lang;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;
import ch.rakudave.jnetmap.view.components.StatusBar;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.*;

public class StatusUpdater {
    private static java.util.Map<Map, ScheduledFuture<?>> maps = new HashMap<>();

    public static void addMap(final Map m) {
        if (m.getUpdateInterval() <= 0) {
            Logger.info("Skipping scheduled task for " + m.getFileName() + " as the update interval is "+ m.getUpdateInterval());
            return;
        }
        if (!maps.containsKey(m)) {
            Logger.debug("Adding scheduled task for " + m.getFileName() + ", running every " + m.getUpdateInterval() + "m");
            maps.put(m, Scheduler.scheduleAtFixedRate(() -> refresh(m), 0, (int) (m.getUpdateInterval() * 60), TimeUnit.SECONDS));
        }
    }

    public static void removeMap(Map m) {
        if (maps.containsKey(m)) {
            maps.remove(m).cancel(true);
            Logger.debug("Removed scheduled task for " + m.getFileName());
        }
    }

    public static void updateTimeInterval(Map m) {
        removeMap(m);
        addMap(m);
    }

    public static void refresh(Map m) {
        Logger.debug("Updating devices on map " + m.getFileName());
        StatusBar.getInstance().setBusy(true);
        ExecutorService es = Executors.newFixedThreadPool(Settings.getInt("status.update.threads", 5));
        Collection<Connection> connections = m.getEdges();
        for (Connection connection : connections) connection.clearStatusMap();
        final Collection<Device> vertecies = m.getVertices();
        final CountDownLatch latch = new CountDownLatch(vertecies.size());
        StatusBar.getInstance().setMessage(Lang.getNoHTML("message.status.update").replaceAll("%name%", m.getFileName() + ".jnm"));
        for (final Device d : vertecies) {
            es.submit(() -> {
                try {
                    d.updateStatus();
                    StatusBar.getInstance().setProgress((int) (((vertecies.size() - latch.getCount()) / (float) vertecies.size()) * 100.0));
                } catch (Exception e) {
                    Logger.error("Unable to update device " + d.getName(), e);
                } finally {
                    latch.countDown();
                }
            });
        }
        es.shutdown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Logger.debug("Failed to await map refresh latch", e);
        } finally {
            StatusBar.getInstance().clearMessage();
            StatusBar.getInstance().setProgress(-1);
            StatusBar.getInstance().setBusy(false);
        }
        // now that we've pinged all the things, there's a good chance it's in the ARP table
        if (Settings.getBoolean("arp.query", true)) {
            Arp.updateArpTable();
            HashMap<String, String> arpTable = Arp.getArpTable();
            for (Device device : m.getVertices()) {
                for (NetworkIF nif : device.getInterfaces()) {
                    if (nif instanceof PhysicalIF && nif.getAddress() != null) {
                        PhysicalIF pif = (PhysicalIF) nif;
                        String macAddress = arpTable.get(pif.getAddress().getHostAddress());
                        if (macAddress != null && !macAddress.equals(pif.getMacAddress())) {
                            pif.setMacAddress(macAddress);
                            Logger.debug("Set MAC address for " + pif.getAddress() + " to " + macAddress);
                            m.setSaved(false);
                        }
                    }
                }
            }
        }
        System.gc();
    }
}
