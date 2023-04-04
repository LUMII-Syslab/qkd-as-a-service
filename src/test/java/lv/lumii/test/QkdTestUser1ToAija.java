package lv.lumii.test;

import lv.lumii.httpws.WsClient;
import lv.lumii.httpws.WsServer;
import lv.lumii.qkd.InjectableQKD;
import lv.lumii.qkd.QkdProperties;
import org.bouncycastle.tls.injection.kems.InjectedKEMs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

public class QkdTestUser1ToAija {


    public static Logger logger; // static initialization

    public static String mainExecutable;
    public static String mainDirectory;

    static {

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
        logger = LoggerFactory.getLogger(QkdTestUser1ToAija.class);

    }

    public static void main(String[] args) throws Exception {

        QkdProperties props = new QkdProperties(mainDirectory);

        System.out.println("TLS provider before2="+InjectableQKD.getTlsProvider());
        InjectableQKD.inject(InjectedKEMs.InjectionOrder.INSTEAD_DEFAULT, props);
        // ^^^ makes BouncyCastlePQCProvider the first and BouncyCastleJsseProvider the second
        System.out.println("TLS provider after="+InjectableQKD.getTlsProvider());

        InjectedKEMs.lockKEM(0xFEFF);
        WsClient wsClient = new WsClient(
                Optional.empty(), new URI("ws://localhost:8001/ws"),
                //props.user1SslFactory(), props.aijaUri(),
                ()-> "Hi, I am User1!1",
                (user2str)-> {System.out.println("User2 replied with: "+user2str);},
                (ex) -> {
            System.out.println("User 2 error: "+ex);
        }, "User1 as a client");
        wsClient.connectBlockingAndRunAsync();
        //wsClient.connectAndRunAsync();

    }


}

