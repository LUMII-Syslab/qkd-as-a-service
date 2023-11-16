package lv.lumii.test;

import lv.lumii.httpws.WsClient;
import lv.lumii.httpws.WsServer;
import lv.lumii.qkd.InjectableQKD;
import lv.lumii.qkd.QkdProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class QkdTestUser1 {


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
        logger = LoggerFactory.getLogger(QkdTestUser1.class);

    }

    public static void main(String[] args) throws Exception {

        QkdProperties props = new QkdProperties(mainDirectory);

        InjectableQKD.inject(false, props);

        long ms1 = System.currentTimeMillis();

        WsClient wsClient = new WsClient(props.user1SslFactory(), props.user2Uri(),
                () -> "Hi, I am User1!",
                (user2str) -> {
                    long ms2 = System.currentTimeMillis();
                    System.out.println("User2 replied with: " + user2str + " time=" + (ms2 - ms1));
                },
                (ex) -> {
                    System.out.println("User 2 error: " + ex);
                }, "User1 as a client");
        wsClient.connectBlockingAndRunAsync();
        //wsClient.connectAndRunAsync();

    }


}

