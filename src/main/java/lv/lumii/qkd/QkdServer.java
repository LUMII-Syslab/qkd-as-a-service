package lv.lumii.qkd;

import lv.lumii.keys.ServerKey;
import org.bouncycastle.pqc.InjectablePQC;
import org.cactoos.Scalar;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Synced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;

/**
 * uses either HttpServer or WsServer inside to serve client requests
 */
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
    private Scalar<Server> server;

    public QkdServer() { // QkdProperties qkdProperties1) {
        System.out.println(" New QkdServer");
        this.server = new Synced<>(new Sticky<>(() -> newConnection() ));
        this.qkdProperties = new QkdProperties(mainDirectory);
    }

    private Server newConnection() throws Exception {

        System.out.println(" New QKD User2 ");

        ServerKey srvKey = qkdProperties.serverKey();

        System.out.println("SRV KEY "+srvKey.password());


        try {
            // set up key manager to do server authentication

            KeyManagerFactory kmf;
            KeyStore ks;
            kmf = KeyManagerFactory.getInstance("SunX509");
            ks = srvKey.keyStore();
            kmf.init(ks, srvKey.password());

            TrustManagerFactory tmf;
            KeyStore ts;
            tmf = TrustManagerFactory.getInstance("SunX509");
            ts = qkdProperties.trustStore();
            tmf.init(ts);

            SSLContext ctx;
            ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), SecureRandom.getInstanceStrong());

             return new HttpServer(ctx, qkdProperties.port());
            //return new WsServer(ctx, qkdProperties.port());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void start() throws Exception {
        this.server.value().start();
    }

}
