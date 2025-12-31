package org.openquantumsafe;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;

public class Common {

    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static void wipe(byte[] array) {
        Arrays.fill(array, (byte) 0);
    }

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isMac() {
        return OS.contains("mac");
    }

    public static boolean isLinux() {
        return OS.contains("nux");
    }

    private static void addMainDirectoryToJavaLibraryPath() {
        File f = new File(Common.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String mainExecutable = f.getAbsolutePath();
        String mainDirectory = f.getParent();

        // Fix for debug purposes (when the code has been launched from the IDE):
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/main")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length() - "/build/classes/java/main".length());
            mainExecutable = "java";
        }

        System.setProperty("java.library.path",
                mainDirectory+File.separator+"lib"+File.pathSeparator+
                        mainDirectory+File.pathSeparator+
                        System.getProperty("java.library.path"));

    }

    private static void tryToLoadLibrary(String name) {
        try {
            System.loadLibrary(name);
            // Otherwise load the library from the liboqs-java.jar
        } catch (UnsatisfiedLinkError e) {
            String libName = name+".so";
            if (Common.isLinux()) {
                libName = "lib"+name+".so";
            } else if (Common.isMac()) {
                libName = "lib"+name+".dylib";//".jnilib";
            } else if (Common.isWindows()) {
                libName = name+".dll";
            }
            URL url = KEMs.class.getResource("/" + libName);
            if (url != null) {
                // try to load from Jar
                File tmpDir;
                try {
                    tmpDir = Files.createTempDirectory("oqs-native-lib").toFile();
                    tmpDir.deleteOnExit();
                    File nativeLibTmpFile = new File(tmpDir, libName);
                    nativeLibTmpFile.deleteOnExit();
                    InputStream in = url.openStream();
                    Files.copy(in, nativeLibTmpFile.toPath());
                    System.load(nativeLibTmpFile.getAbsolutePath());
                    return; // all ok
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            // try to load from java.library.path
            String[] paths = System.getProperty("java.library.path").split(File.pathSeparator);
            for (String path : paths) {
                File f = new File(path+File.separator+libName);
                if (f.isFile()) {
                    try {
                        url = f.toURI().toURL();
                        System.load(url.getFile()); // load from full file name
                        return;
                    } catch (Throwable exception) {
                    }
                }
            }

            throw new RuntimeException("Could not find "+libName+" in java.library.path or in the project or jar directory.");
            // e.g., download from:
            // https://qkd.lumii.lv/liboqs-binaries/Darwin-x86_64/liboqs.dylib
            // https://qkd.lumii.lv/liboqs-binaries/Darwin-x86_64/liboqs-jni.dylib
        }
    }

    public static void loadNativeLibrary() {
        addMainDirectoryToJavaLibraryPath();

        tryToLoadLibrary("oqs");
        // oqs-jni depends on liboqs but sometimes is not able to load it on MacOS

        tryToLoadLibrary("oqs-jni");

    }

    public static <E, T extends Iterable<E>> void print_list(T list) {
        for (Object element : list){
            System.out.print(element);
            System.out.print(" ");
        }
        System.out.println();
    }

    public static String to_hex(byte[] bytes) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            int v = aByte & 0xFF;
            sb.append(HEX_ARRAY[v >>> 4]);
            sb.append(HEX_ARRAY[v & 0x0F]);
            sb.append(" ");
        }
        return sb.toString();
    }

    public static String chop_hex(byte[] bytes) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder();
        int num = 8;
        for (int i = 0; i < num; i++) {
            int v = bytes[i] & 0xFF;
            sb.append(HEX_ARRAY[v >>> 4]);
            sb.append(HEX_ARRAY[v & 0x0F]);
            sb.append(" ");
        }
        if (bytes.length > num*2) {
            sb.append("... ");
        }
        for (int i = bytes.length - num; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            sb.append(HEX_ARRAY[v >>> 4]);
            sb.append(HEX_ARRAY[v & 0x0F]);
            sb.append(" ");
        }
        return sb.toString();
    }

}