package lv.lumii.test;


import lv.lumii.httpws.WsServer;
import lv.lumii.httpws.WsSink;
import lv.lumii.pqc.InjectablePQC;
import lv.lumii.qkd.InjectableQKD;
import lv.lumii.qkd.QkdProperties;
import org.bouncycastle.tls.injection.kems.InjectedKEMs;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Optional;

public class QkdTestAijaForUser1 {

    public static Logger logger; // static initialization

    public static String mainExecutable;
    public static String mainDirectory;

    static {

        File f = new File(QkdTestAijaForUser1.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        mainExecutable = f.getAbsolutePath();
        mainDirectory = f.getParent();

        // Fix for debug purposes when qkd-client is launched from the IDE:
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/main")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length() - "/build/classes/java/main".length());
            mainExecutable = "java";
        }
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/test")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length() - "/build/classes/java/test".length());
            mainExecutable = "java";
        }

        String logFileName = mainDirectory + File.separator + "wsserver.log";
        System.setProperty("org.slf4j.simpleLogger.logFile", logFileName);
        logger = LoggerFactory.getLogger(QkdTestAijaForUser1.class);
    }

    public static void main(String[] args) throws Exception {

        //QkdProperties qkdProperties = new QkdProperties(mainDirectory);

        /*System.out.println("TLS provider before="+InjectableQKD.getTlsProvider());
        InjectableQKD.inject(InjectedKEMs.InjectionOrder.INSTEAD_DEFAULT, qkdProperties);
        // ^^^ makes BouncyCastlePQCProvider the first and BouncyCastleJsseProvider the second
        System.out.println("TLS provider after="+InjectableQKD.getTlsProvider());*/
        //InjectablePQC.inject(InjectedKEMs.InjectionOrder.INSTEAD_DEFAULT);

        //SSLContext ctx = qkdProperties.user2SslContext();

        int port = 8001;//qkdProperties.user2Uri().getPort();

        System.out.println("AIJA 1server port=" + port);
        WsServer srv = new WsServer(
                //Optional.of(ctx),
                Optional.empty(),
                port,
                (WebSocket client) -> new WsSink() {

                    @Override
                    public void open(WebSocket ws) {
                        System.out.println("AIJA for User 1: connection opened");
                        client.send("Connected: I am AIJA!");
                    }

                    @Override
                    public void consumeMessage(String s) {
                        System.out.println("AIJA for User1 received string: " + s);
                        client.send("AIJA reply for: [" + s + "]");
                    }

                    @Override
                    public void consumeMessage(ByteBuffer blob) {
                        System.out.println("AIJA for User1 received: binary data of " + blob.array().length + " bytes");
                    }

                    @Override
                    public void closeGracefully(String details) {
                        System.out.println("AIJA: connection closed");
                    }

                    @Override
                    public void closeWithException(Exception e) {
                        System.out.println("AIJA: connection exception");
                    }
                });

        srv.start();

    }


}
