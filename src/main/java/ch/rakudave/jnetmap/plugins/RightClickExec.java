package ch.rakudave.jnetmap.plugins;

import ch.rakudave.jnetmap.controller.Scheduler;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.plugins.extensions.RightClickAction;
import ch.rakudave.jnetmap.util.IO;
import ch.rakudave.jnetmap.util.Icons;
import ch.rakudave.jnetmap.util.SwingHelper;
import ch.rakudave.jnetmap.util.logging.Logger;

import javax.swing.*;
import java.util.Arrays;

public class RightClickExec implements RightClickAction {
    private String name, command, args;

    private RightClickExec() {
    }

    public RightClickExec(String name, String command, String args) {
        this();
        this.name = name;
        this.command = command;
        this.args = args;
    }

    /**
     * This command will be executed when a user right-clicks a device and selects this item.
     *
     * @param d that device that was right-clicked, use this to extract the address etc.
     */
    public void execute(final Device d) {
        Scheduler.execute(() -> {
            Process process = null;
            try {
                String[] commandAndArgs = IO.splitCommandArgs(command, fillArgs(args, d));
                Logger.debug("Executing script: " + Arrays.toString(commandAndArgs));
                process = Runtime.getRuntime().exec(commandAndArgs);
                process.waitFor();
            } catch (Exception ex) {
                Logger.warn("Unable to execute script", ex);
            } finally {
                if (process != null) process.destroy();
            }
        });
    }

    private String fillArgs(String args, Device d) {
        if (args == null || "".equals(args)) return "";
        String result = args.replaceAll("%s", d.getStatus().toString())
                .replaceAll("%n", d.getName())
                .replaceAll("%t", d.getType().toString())
                .replaceAll("%d", d.getDesctription())
                .replaceAll("%l", d.getLocation())
                .replaceAll("%m", d.getModel())
                .replaceAll("%v", d.getVendor());
        if (args.contains("%a")) result = result.replaceAll("%a", SwingHelper.interfaceSelector(d, false) + "");
        if (args.contains("%h")) result = result.replaceAll("%h", SwingHelper.interfaceSelector(d, true) + "");
        return result;
    }

    public String getName() {
        return name;
    }

    @Override
    public Icon getIcon() {
        return Icons.get("script");
    }
}
