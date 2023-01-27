package lv.lumii.qkd;

import lv.lumii.qrng.QrngClient;
import org.bouncycastle.pqc.InjectablePQC;
import org.cactoos.Scalar;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Synced;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;

public class WsServer {

    public static Logger logger; // one common logger

    public static String mainExecutable;
    public static String mainDirectory;

    static {

        InjectablePQC.inject(); // makes BouncyCastlePQCProvider the first and BouncyCastleJsseProvider the second

        File f = new File(WsServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        mainExecutable = f.getAbsolutePath();
        mainDirectory = f.getParent();

        // Fix for debug purposes when qrng-client is launched from the IDE:
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/main")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length()-"/build/classes/java/main".length());
            mainExecutable = "java";
        }

        String logFileName = mainDirectory+ File.separator+"qkd.log";
        System.setProperty("org.slf4j.simpleLogger.logFile", logFileName);
        logger = LoggerFactory.getLogger(QrngClient.class);

        Provider tlsProvider = null;
        try {
            tlsProvider = SSLContext.getInstance("TLS").getProvider();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        logger.info("QkdServer is using TLS provider: "+tlsProvider.getName()); // BCJSSE

    }

    //private QkdProperties qkdProperties;
    //private ClientBuffer clientBuffer;

    private int port = 2222;
    private Scalar<WebSocketServer> wsserver;

    public WsServer() { // QkdProperties qkdProperties1) {
        System.out.println(" New QkdServer");
        this.wsserver = new Synced<>(new Sticky<>(() -> newConnection() ));

    }

    private WebSocketServer newConnection() throws Exception {

        System.out.println(" New QKD User2 ");

        //QrngClientToken token = qrngProperties.clientToken();

        //System.out.println("TOKEN "+token.password()+" "+token.certificateChain());

        //TrustManagerFactory trustMgrFact = TrustManagerFactory.getInstance("SunX509");
        //trustMgrFact.init(qrngProperties.trustStore());

        /*SSLFactory sslf2 = SSLFactory.builder()
                //.withIdentityMaterial(token.key(), token.password(), token.certificateChain())
                //.withNeedClientAuthentication()
                //.withWantClientAuthentication()
                .withProtocols("TLSv1.3")
                //.withTrustMaterial(trustMgrFact)
                .withSecureRandom(SecureRandom.getInstanceStrong())
                .withCiphers("TLS_AES_256_GCM_SHA384")
                .build();
*/


        WebSocketServer wssrv = new WebSocketServer(new InetSocketAddress(port)) {

            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                System.out.println("Handshake :" + handshake.toString());
            }

            @Override
            public void onMessage(WebSocket arg0, String arg1) {
                System.out.println("Server receives:  " + arg1 + " " + arg0.getRemoteSocketAddress());

                    //SERVER SEND THE CLIENT ID AND REGISTER A NEW CONNECTION
                    //clientSockets.add(new Connection(arg0, clientId));

                    //clientId++;
                    //nClients++;

            }

            @Override
            public void onError(WebSocket arg0, Exception arg1) {
                // TODO Auto-generated method stub
                System.out.println("Server Error " + arg1);
            }

            @Override
            public void onStart() {

            }

            @Override
            public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
            }
        };

        wssrv.setConnectionLostTimeout(20);
        //wssrv.set .setSocketFactory(sslf2.getSslSocketFactory());
        //Start Server functionality
        wssrv.start();
        System.out.println("Server started and ready.");

        return wssrv;
    }

    public void start() {
        try {
            this.wsserver.value(); // init the value
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
