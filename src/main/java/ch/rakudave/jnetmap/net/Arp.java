package ch.rakudave.jnetmap.net;

import ch.rakudave.jnetmap.util.IO;
import ch.rakudave.jnetmap.util.Settings;
import ch.rakudave.jnetmap.util.logging.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Arp {
    private static HashMap<String, String> arpTable;

    public static void updateArpTable() {
        arpTable = new HashMap<>();
        Process process = null;
        try {
            String command = (IO.isUnix) ? Settings.get("arp.syntax.unix", "arp -n") : Settings.get("arp.syntax.windows", "arp -a");
            Logger.trace("Executing " + command);
            process = Runtime.getRuntime().exec(command);
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) parseLine(line);
            Logger.trace("Exit code " + process.waitFor());
        } catch (Exception e) {
            Logger.debug("Failed to read arp table", e);
        } finally {
            if (process != null) process.destroy();
        }
    }

    public static HashMap<String, String> getArpTable() {
        return arpTable;
    }

    private static void parseLine(String line) {
        Pattern ipPattern = Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");
        Matcher ipMatcher = ipPattern.matcher(line);
        Pattern macPattern = Pattern.compile("([0-9a-fA-F]{1,2}[\\.:-]){5}([0-9a-fA-F]{1,2})");
        Matcher macMatcher = macPattern.matcher(line);
        if (ipMatcher.find() && macMatcher.find()) {
            arpTable.put(ipMatcher.group(), macMatcher.group().replaceAll("[\\.-]", ":"));
        }
    }
}
