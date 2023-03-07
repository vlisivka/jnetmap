package ch.rakudave.jnetmap.util;

import ch.rakudave.jnetmap.model.Connection;
import ch.rakudave.jnetmap.model.device.Device;
import ch.rakudave.jnetmap.plugins.JNetMapPlugin;
import ch.rakudave.jnetmap.util.logging.Logger;
import edu.uci.ics.jung.visualization.VisualizationViewer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author rakudave
 */
public class IO {
    private static IO instance = null;
    private static final int bufferSize = 4096;
    public static final File userDir = new File(System.getProperty("user.home") + "/.jNetMap");
    public static final String pluginDirName = "/plugins", langDirName = "/lang", iconsDirName = "/icons", devicesDirName = "/devices";
    public static final boolean isUnix = !System.getProperty("os.name").toLowerCase().startsWith("windows");
    public static final boolean isOSX = System.getProperty("os.name").toLowerCase().startsWith("mac");
    public static final boolean isLinux = isUnix && !isOSX;

    private IO() {
    }

    private static Class instance() {
        if (instance == null) instance = new IO();
        return instance.getClass();
    }

    public static URL getResource(String path) {
        return instance().getResource(path);
    }

    public static InputStream getResourceAsStream(String path) {
        return instance().getResourceAsStream(path);
    }

    private static List<String> listURI(URI uri, boolean dirsOnly) {
        List<String> files = new ArrayList<>();
        try (FileSystem fileSystem = (uri.getScheme().equals("jar") ? FileSystems.newFileSystem(uri, Collections.emptyMap()) : null)) {
            Path path = (fileSystem != null)?fileSystem.getPath(uri.toString().split("!")[1]):Paths.get(uri);
            Files.walkFileTree(path, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if ((!dirsOnly || attrs.isDirectory()) && !(file.startsWith(".") || file.endsWith("~"))) {
                        files.add(file.getFileName().toString().replaceAll("/$", ""));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch(Exception e) {
            Logger.error("Failed to list resources in "+uri.toString());
        }
        return files;
    }

    public static Set<String> listFiles(String path, boolean dirsOnly) {
        Set<String> files = new TreeSet<>();
        try {
            files.addAll(listURI(getResource(path).toURI(), dirsOnly));
        } catch (Exception e) {
            Logger.warn("Failed to list internal resources for " + path, e);
        }
        try {
            File userDir = new File(IO.userDir, path);
            if (userDir.exists()) files.addAll(listURI(userDir.toURI(), dirsOnly));
        } catch (Exception e) {
            Logger.warn("Failed to list user resources for " + path, e);
        }
        return files;
    }

    public static Properties getMergedProps(String path) {
        Properties internal = new Properties();
        Properties custom = new Properties();
        try {
            InputStream rs = IO.getResourceAsStream(path);
            if (rs != null) internal.load(rs);
        } catch (Exception e) {
            Logger.error("Failed to load props from resources " + path, e);
        }
        File customFile = new File(IO.userDir, path);
        if (customFile.exists()) {
            try {
                custom.load(new FileInputStream(customFile));
                internal.putAll(custom);
            } catch (Exception e) {
                Logger.warn("Failed to load props from user dir " + path, e);
            }
        }
        return internal;
    }

    /**
     * Copy a file or directory recursively
     *
     * @param source     source file
     * @param dest       destination file
     * @param ignoreList file-names to ignore
     * @return success
     */
    public static boolean copy(File source, File dest, List<String> ignoreList) {
        if (ignoreList != null && ignoreList.contains(source.getName())) return true;
        boolean result = true;
        if (source.isDirectory()) {
            Logger.trace("Copying dir " + dest.getName());
            dest.mkdirs();
            String list[] = source.list();
            if (list != null) {
                for (String aList : list) {
                    result &= copy(new File(source, aList), new File(dest, aList), ignoreList);
                }
            }
        } else {
            if (dest.exists() && dest.lastModified() > source.lastModified()) return true;
            Logger.trace("Copying file " + dest.getName());
            try (FileInputStream fin = new FileInputStream(source); FileOutputStream fout = new FileOutputStream(dest)) {
                result = copy(fin, fout);
            } catch (IOException e) {
                Logger.error("Failed to open I/O-Stream(s)", e);
                return false;
            }
        }
        return result;
    }

    public static boolean copy(InputStream in, OutputStream out) {
        try {
            byte[] buf = new byte[bufferSize];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return true;
        } catch (IOException e) {
            Logger.error("Failed to open I/O-stream(s)", e);
        } finally {
            try {
                in.close();
                out.close();
            } catch (Exception ignored) {}
        }
        return false;
    }

    public static boolean copy(InputStream in, File dest) {
        try (FileOutputStream out = new FileOutputStream(dest))  {
            return copy(in, out);
        } catch (Exception e) {
            Logger.error("Failed to open output-stream", e);
            return false;
        }
    }

    /**
     * create user directory and install preferences, languages and icons
     */
    public static void updateUserFiles() {
        Logger.info("User directory: " + userDir.getAbsolutePath());
        new File(userDir, langDirName).mkdirs();
        new File(userDir, iconsDirName).mkdirs();
        new File(userDir, devicesDirName).mkdirs();
        File plugins = new File(userDir, pluginDirName);
        plugins.mkdirs();
        Arrays.stream(plugins.listFiles()) // remove legacy plugins
            .filter(file -> file.getName().endsWith(".jar") && !file.getName().contains("-"))
            .forEach(file -> file.delete());
        try {
            URL pluginResource = getResource(pluginDirName);
            if (pluginResource != null) {
                listURI(pluginResource.toURI(), false) // update default plugins
                        .forEach(s -> IO.copy(getResourceAsStream(pluginDirName +"/"+s), new File(plugins, s)));
            } else {
                Logger.debug("No bundled plugins found");
            }
        } catch (Exception e) {
            Logger.warn("Failed to update default plugins", e);
        }
    }

    public static boolean unzip(File zipFile, File destination) {
        if (zipFile == null || (!zipFile.exists() || !zipFile.canRead()
                || !zipFile.getName().contains(".zip"))) return false;
        try (ZipFile zip = new ZipFile(zipFile)) {
            boolean success = true;
            Enumeration<? extends ZipEntry> oEnum = zip.entries();
            while (oEnum.hasMoreElements()) {
                ZipEntry zipEntry = oEnum.nextElement();
                if (zipEntry.isDirectory()) {
                    success &= (new File(destination.getAbsoluteFile() + "/" + zipEntry.getName())).mkdir();
                } else {
                    success &= copy(zip.getInputStream(zipEntry), new File(destination,zipEntry.getName()));
                }
            }
            return success;
        } catch (IOException e) {
            Logger.error("Failed to open ZIP-file", e);
            // attempt cleanup
            (new File(destination, zipFile.getName().replace(".zip", ""))).delete();
            return false;
        }
    }

    public static boolean exportImage(VisualizationViewer<Device, Connection> in, File out) {
        System.out.println(in.getSize());
        BufferedImage buff = new BufferedImage(in.getWidth(), in.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics g = buff.getGraphics();
        in.setDoubleBuffered(false);
        in.paintAll(g);
        in.setDoubleBuffered(true);
        g.dispose();
        try {
            ImageIO.write(buff, "png", out);
            return true;
        } catch (IOException e) {
            Logger.error("Unable to export image", e);
            return false;
        }
    }

    public static String getString(File file) {
        if (file == null || !file.exists()) return null;
        Writer writer = new StringWriter();
        char[] buffer = new char[bufferSize];
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (Exception e) {
            Logger.error("Unable to read string", e);
            return null;
        }
        return writer.toString();
    }

    @Deprecated
    public static boolean tryRemovePlugin(JNetMapPlugin p) {
        // since jspf does not know where a plugin came from, lets do this the hard way
        // (aka. open every jar an look for matching class names)
        for (File f : new File(IO.userDir, "plugins").listFiles()) {
            if (f.getName().contains(".jar")) {
                try (ZipFile zip = new ZipFile(f)) {
                    Enumeration<? extends ZipEntry> oEnum = zip.entries();
                    while (oEnum.hasMoreElements()) {
                        ZipEntry zipEntry = oEnum.nextElement();
                        if (!zipEntry.isDirectory() && zipEntry.getName().contains(
                                p.getClass().getSimpleName() + ".class")) {
                            return f.delete();
                        }
                    }
                } catch (Exception ex) {
                    Logger.warn("Unable to read jar-file " + f, ex);
                }
            }
        }
        return false;
    }


    /**
     * @author Jan Goyvaerts
     */
    public static String[] splitCommandArgs(String command, String args) {
        List<String> matchList = new ArrayList<>();
        matchList.add(command);
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(args);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }
        String[] out = new String[matchList.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = matchList.get(i);
        }
        return out;
    }
}