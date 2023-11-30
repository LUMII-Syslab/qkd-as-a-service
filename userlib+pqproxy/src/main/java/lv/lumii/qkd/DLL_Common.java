package lv.lumii.qkd;


import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DLL_Common {

    public static Logger logger;

    public static CCharPointer NULL_BUFFER;
    private static String mainExecutable;
    private static String mainDirectory;
    /*package-visibility*/ static QkdProperties props;


    static {

        try {
            NULL_BUFFER = WordFactory.nullPointer(); // used from GraalVM native image
        } catch (Exception e) {
            NULL_BUFFER = null; // used from Java mode of GraalVM
        }


        File f = new File(DLL_Common.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        mainExecutable = f.getAbsolutePath();
        mainDirectory = f.getParent();

        // Fix for debug purposes when qrng-client is launched from the IDE:
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/main")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length() - "/build/classes/java/main".length());
            mainExecutable = "java";
        }


        String logFileName = mainDirectory + File.separator + "log" + File.separator + "qaas-" + new Date().getTime() + ".log";
        new File(logFileName).getParentFile().mkdirs();
        System.setProperty("org.slf4j.simpleLogger.logFile", logFileName);
        logger = LoggerFactory.getLogger(DLL_Common.class);

        props = new QkdProperties(mainDirectory);
        InjectableQKD.inject(props);

    }


    /**
     * Technical function to transform Java String to CCharPointer (char*) that can be returned to C.
     * The result must be freed by calling qrng_free_result.
     *
     * @param string the Java string to transform
     * @return a CCharPointer that will be returned to C as char*
     */
    /*package-visibility*/
    static CCharPointer toCCharPointer(String string) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        CCharPointer charPointer = UnmanagedMemory.calloc((bytes.length + 1) * SizeOf.get(CCharPointer.class));
        for (int i = 0; i < bytes.length; ++i) {
            charPointer.write(i, bytes[i]);
        }
        charPointer.write(bytes.length, (byte) 0);
        return charPointer;
    }

    /**
     * Technical function to transform Java byte[] to CCharPointer (char*) that can be returned to C.
     * The result must be freed by calling qrng_free_result.
     *
     * @param bytes the Java byte array to transform
     * @return a CCharPointer that will be returned to C as char*
     */
    /*package-visibility*/
    static CCharPointer toCCharPointer(byte[] bytes) {
        CCharPointer charPointer = UnmanagedMemory.calloc((bytes.length + 1) * SizeOf.get(CCharPointer.class));
        for (int i = 0; i < bytes.length; ++i) {
            charPointer.write(i, bytes[i]);
        }
        charPointer.write(bytes.length, (byte) 0);
        return charPointer;
    }

    /*package-visibility*/
    static String stringFromCCharPointer(CCharPointer ptr) {
        ArrayList<Byte> arr = new ArrayList<>();
        int i = 0;
        for (; ; ) {
            byte b = ptr.read(i);
            i++;
            if (b == 0) { // end-of-string
                break;
            }
            arr.add(b);
        }
        byte[] bb = new byte[arr.size()];
        for (i = 0; i < arr.size(); i++)
            bb[i] = arr.get(i);

        return new String(bb, StandardCharsets.UTF_8);
    }

    /*package-visibility*/
    static byte[] byteArrayFromCCharPointer(CCharPointer ptr, int len) {
        byte[] bb = new byte[len];
        for (int i = 0; i < len; i++)
            bb[i] = ptr.read(i);
        return bb;
    }

    @CEntryPoint(name = "qaas_free_result")
    /*package-visibility*/ static synchronized void qaas_free_result(IsolateThread thread, CCharPointer result) {
        if (result.isNonNull()) UnmanagedMemory.free(result);
    }

}
