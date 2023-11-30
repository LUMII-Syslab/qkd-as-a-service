package lv.lumii.test;


import lv.lumii.httpws.WsServer;
import lv.lumii.httpws.WsSink;
import lv.lumii.qkd.InjectableQKD;
import lv.lumii.qkd.QkdProperties;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Optional;

public class QkdTestUser2 {

    public static Logger logger; // static initialization

    public static String mainExecutable;
    public static String mainDirectory;

    static {

        File f = new File(QkdTestUser2.class.getProtectionDomain().getCodeSource().getLocation().getPath());
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
        logger = LoggerFactory.getLogger(QkdTestUser2.class);
    }

    public static void main(String[] args) throws Exception {

        QkdProperties qkdProperties = new QkdProperties(mainDirectory);

        InjectableQKD.inject(qkdProperties);

        SSLContext ctx = qkdProperties.user2SslContext();

        int port = qkdProperties.user2Uri().getPort();

        System.out.println("User2 server port=" + port);
        WsServer srv = new WsServer(
                Optional.of(ctx),
                port,
                (WebSocket client) -> new WsSink() {

                    @Override
                    public void open(WebSocket ws) {
                        System.out.println("User 1: connection opened");
                        client.send("Connected: I am User2!");
                    }

                    @Override
                    public void consumeMessage(String s) {
                        System.out.println("From User1: " + s);
                        client.send("User 2 reply for: [" + s + "]");
                    }

                    @Override
                    public void consumeMessage(ByteBuffer blob) {
                        System.out.println("From User1: binary data of " + blob.array().length + " bytes");
                    }

                    @Override
                    public void closeGracefully(String details) {
                        System.out.println("User2: connection closed");
                    }

                    @Override
                    public void closeWithException(Exception e) {
                        System.out.println("User2: connection exception");
                    }
                });

        srv.start();

    }


}
