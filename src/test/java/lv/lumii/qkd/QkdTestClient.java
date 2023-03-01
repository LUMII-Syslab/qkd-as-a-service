package lv.lumii.qkd;

import org.bouncycastle.pqc.InjectablePQC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;

public class QkdTestClient {


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

    public static void main(String[] args) throws Exception {
        QkdProperties props = new QkdProperties(mainDirectory);

        SSLContext ctx;

        TrustManagerFactory tmf;

        ctx = SSLContext.getInstance("TLS");
        tmf = TrustManagerFactory.getInstance("SunX509");

        tmf.init(props.trustStore());
        ctx.init(null, tmf.getTrustManagers(), SecureRandom.getInstanceStrong());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(props.remoteUri())
                //.version(HttpClient.Version.HTTP_2)
                .GET()
                .build();

        HttpClient client = HttpClient
                .newBuilder()
                .sslContext(ctx)
                //.proxy(ProxySelector.getDefault())
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("SMART RESPONSE: " + response.body());
        }catch (Exception e ){
            e.printStackTrace();
        }

    }


}
