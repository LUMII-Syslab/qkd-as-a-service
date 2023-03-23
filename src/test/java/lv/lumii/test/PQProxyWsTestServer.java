package lv.lumii.test;


import lv.lumii.httpws.WsServer;
import lv.lumii.httpws.WsSink;
import lv.lumii.qkd.InjectableQKD;
import lv.lumii.qkd.QkdProperties;
import lv.lumii.pqc.InjectablePQC;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.tls.injection.kems.InjectedKEMs;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Optional;

public class PQProxyWsTestServer {

    public static Logger logger; // static initialization

    public static String mainExecutable;
    public static String mainDirectory;

    static {

        InjectableQKD.inject(InjectedKEMs.InjectionOrder.AFTER_DEFAULT);

        File f = new File(PQProxyWsTestServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        mainExecutable = f.getAbsolutePath();
        mainDirectory = f.getParent();

        // Fix for debug purposes when qkd-client is launched from the IDE:
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/main")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length()-"/build/classes/java/main".length());
            mainExecutable = "java";
        }
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/test")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length()-"/build/classes/java/test".length());
            mainExecutable = "java";
        }

        String logFileName = mainDirectory+ File.separator+"wsserver.log";
        System.setProperty("org.slf4j.simpleLogger.logFile", logFileName);
        logger = LoggerFactory.getLogger(PQProxyWsTestServer.class);

        Provider tlsProvider = null;
        try {
            tlsProvider = SSLContext.getInstance("TLS").getProvider();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        logger.info("Using TLS provider: "+tlsProvider.getName()); // BCJSSE

    }

    public static void main(String[] args) throws Exception {

        QkdProperties qkdProperties = new QkdProperties(mainDirectory);
        SSLContext ctx = qkdProperties.serverSslContext();

        int port = qkdProperties.serverUri().getPort();

        System.out.println("Ws test server port="+port);
        WsServer srv = new WsServer(
                Optional.of(ctx),
                port,
                (WebSocket client) -> new WsSink() {

                    @Override
                    public void open(WebSocket ws) {
                        System.out.println("WS test server: connection opened");
                        client.send("Connected: I am WS test server!");
                        new Thread(()->{
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            client.send("Connected and waited: I am WS test server!!");
                        }).start();
                    }

                    @Override
                    public void consumeMessage(String s) {
                        System.out.println("WS test server: message received: "+s);
                        client.send("WS test server reply for: ["+s+"]");
                        new Thread(()->{
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            client.send("WS test server delayed reply for: ["+s+"]");
                        }).start();
                    }

                    @Override
                    public void consumeMessage(ByteBuffer blob) {

                    }

                    @Override
                    public void closeGracefully(String details) {
                        System.out.println("WS test server: closed");
                    }

                    @Override
                    public void closeWithException(Exception e) {
                        System.out.println("WS test server: exception");
                    }
                });

        srv.start();

    }


}
