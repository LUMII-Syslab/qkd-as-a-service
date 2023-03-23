package lv.lumii.test;

import lv.lumii.httpws.WsClient;
import lv.lumii.httpws.WsServer;
import lv.lumii.httpws.WsSink;
import lv.lumii.qkd.InjectableQKD;
import lv.lumii.qkd.QkdProperties;
import org.bouncycastle.tls.injection.kems.InjectedKEMs;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.ByteBuffer;

public class QkdTestUser1 {


    public static Logger logger; // static initialization

    public static String mainExecutable;
    public static String mainDirectory;

    static {

        InjectableQKD.inject(InjectedKEMs.InjectionOrder.INSTEAD_DEFAULT);
        // ^^^ makes BouncyCastlePQCProvider the first and BouncyCastleJsseProvider the second

        File f = new File(WsServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        mainExecutable = f.getAbsolutePath();
        mainDirectory = f.getParent();

        // Fix for debug purposes when qkd-client is launched from the IDE:
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/main")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length() - "/build/classes/java/main".length());
            mainExecutable = "java";
        }

        String logFileName = mainDirectory + File.separator + "user1.log";
        System.setProperty("org.slf4j.simpleLogger.logFile", logFileName);
        logger = LoggerFactory.getLogger(QkdTestUser1.class);

    }

    public static void main(String[] args) throws Exception {

        QkdProperties props = new QkdProperties(mainDirectory);

        WsSink replySink = new WsSink() {
            @Override
            public void open(WebSocket ws) {
                System.out.println("REPLY SINK OPEN OK");
            }

            @Override
            public void consumeMessage(String s) {
                System.out.println("From User2: " + s);
            }

            @Override
            public void consumeMessage(ByteBuffer blob) {
                System.out.println("From User2: binary data of " + blob.array().length + " bytes");
            }

            @Override
            public void closeGracefully(String details) {
                System.out.println("User1: connection closed");
            }

            @Override
            public void closeWithException(Exception e) {
                System.out.println("User1: connection exception");
            }
        };

        WsClient wsClient = new WsClient(props.targetSslFactory(), props.serverUri(), replySink);
        wsClient.connectBlockingAndRunAsync();

    }


}

