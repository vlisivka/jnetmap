package ch.rakudave.jnetmap.controller;

import ch.rakudave.jnetmap.util.logging.Logger;
import org.apache.commons.cli.CommandLine;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Detects if another instance is already running and sends it the files to
 * open. If no other instance is running, a ServerSocket is opened to listen for
 * incoming requests.
 *
 * @author rakudave
 */
public class InstanceDetector {
    private static final int PORT = 61337;

    public static void delegate(CommandLine cmd) {
        try {
            // try to open a socket. If this fails, there's a reasonable chance
            // there's another running instance of 'myself' using the same port.
            final ServerSocket s = new ServerSocket(PORT, 0, InetAddress.getLocalHost());
            new Thread(() -> {
                while (true) {
                    try {
                        Socket receive = s.accept();
                        CommandLine sentArgs = (CommandLine) new ObjectInputStream(receive.getInputStream()).readObject();
                        if (sentArgs.hasOption("c")) {
                            receive.close();
                            s.close();
                            Controller.shutdown(false);
                        } else {
                            Controller.openRequestedFiles(sentArgs, true);
                        }
                    } catch (Exception e) {
                        Logger.error("Error in socket communication", e);
                    }
                }
            }).start();
        } catch (IOException e) {
            try (Socket send = new Socket(InetAddress.getLocalHost(), PORT)) { // try to send the other instance of myself the files to open and quit
                System.out.println("Found another running instance, sending arguments");
                new ObjectOutputStream(send.getOutputStream()).writeObject(cmd);
                System.out.println("Successfully delegated, shutting down...");
                System.exit(0);
            } catch (IOException ex) {
                System.out.println("Failed to communicate with the other instance. Unable to delegate, running own instance...");
                Logger.error("Failed to delegate", e);
            }
        }
    }

    private InstanceDetector() {
    }
}
