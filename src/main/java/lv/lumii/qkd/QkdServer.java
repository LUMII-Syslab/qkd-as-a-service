package lv.lumii.qkd;

import org.bouncycastle.pqc.InjectablePQC;
import org.cactoos.Scalar;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Synced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.File;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;

public class QkdServer {

    public static Logger logger; // static initialization

    public static String mainExecutable;
    public static String mainDirectory;

    static {

        InjectablePQC.inject(); // makes BouncyCastlePQCProvider the first and BouncyCastleJsseProvider the second

        File f = new File(QkdServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        mainExecutable = f.getAbsolutePath();
        mainDirectory = f.getParent();

        // Fix for debug purposes when qkd-client is launched from the IDE:
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/main")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length()-"/build/classes/java/main".length());
            mainExecutable = "java";
        }

        String logFileName = mainDirectory+ File.separator+"qkd.log";
        System.setProperty("org.slf4j.simpleLogger.logFile", logFileName);
        logger = LoggerFactory.getLogger(QkdServer.class);

        Provider tlsProvider = null;
        try {
            tlsProvider = SSLContext.getInstance("TLS").getProvider();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        logger.info("QkdServer is using TLS provider: "+tlsProvider.getName()); // BCJSSE

    }

    private QkdProperties qkdProperties;

    private int port = 2222;
    private Scalar<TlsServer> wsserver;

    public QkdServer() { // QkdProperties qkdProperties1) {
        System.out.println(" New QkdServer");
        this.wsserver = new Synced<>(new Sticky<>(() -> newConnection() ));
        this.qkdProperties = new QkdProperties(mainDirectory);
    }

    private TlsServer newConnection() throws Exception {

        System.out.println(" New QKD User2 ");

        QkdServerKey srvKey = qkdProperties.serverKey();

        System.out.println("SRV KEY "+srvKey.password());


        SSLServerSocketFactory ssf = null;
        try {
            // set up key manager to do server authentication
            SSLContext ctx;

            KeyManagerFactory kmf;
            KeyStore ks;

            ctx = SSLContext.getInstance("TLS");
            kmf = KeyManagerFactory.getInstance("SunX509");
            ks = srvKey.keyStore();

            kmf.init(ks, srvKey.password());
            ctx.init(kmf.getKeyManagers(), null, SecureRandom.getInstanceStrong());

            ssf = ctx.getServerSocketFactory();

            ServerSocket ss = ssf.createServerSocket(port);
            return new TlsServer(ss);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void start() {
        try {
            this.wsserver.value(); // init the value
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
