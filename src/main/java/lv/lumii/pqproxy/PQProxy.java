package lv.lumii.pqproxy;

import lv.lumii.keys.ClientKey;
import lv.lumii.keys.ServerKey;
import lv.lumii.qkd.QkdServer;
import nl.altindag.ssl.SSLFactory;
import org.bouncycastle.pqc.InjectablePQC;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;

public class PQProxy {


    public static Logger logger; // static initialization

    public static String mainExecutable;
    public static String mainDirectory;

    static {

        InjectablePQC.inject(); // makes BouncyCastlePQCProvider the first and BouncyCastleJsseProvider the second

        File f = new File(SourceWsServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        mainExecutable = f.getAbsolutePath();
        mainDirectory = f.getParent();

        // Fix for debug purposes when qkd-client is launched from the IDE:
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/main")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length()-"/build/classes/java/main".length());
            mainExecutable = "java";
        }

        String logFileName = mainDirectory+ File.separator+"pqproxy.log";
        System.setProperty("org.slf4j.simpleLogger.logFile", logFileName);
        logger = LoggerFactory.getLogger(QkdServer.class);

        Provider tlsProvider = null;
        try {
            tlsProvider = SSLContext.getInstance("TLS").getProvider();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        logger.info("PQProxy is using TLS provider: "+tlsProvider.getName()); // BCJSSE

    }

    private static KeyManager[] loadKeyManager(PQProxyProperties props) throws Exception {
        KeyManagerFactory kmfSourceServer = KeyManagerFactory.getInstance("SunX509");
        ServerKey k = props.sourceServerKey();
        kmfSourceServer.init(k.keyStore(), k.password());
        return kmfSourceServer.getKeyManagers();
    }

    private static TrustManager[] loadAllTrustManagers(PQProxyProperties props) throws Exception {
        TrustManagerFactory tmfServer, tmfClient;
        tmfServer = TrustManagerFactory.getInstance("SunX509");
        tmfServer.init(props.sourceCaTrustStore());
        tmfClient = TrustManagerFactory.getInstance("SunX509");
        tmfClient.init(props.targetCaTrustStore());

        TrustManager[] a = tmfServer.getTrustManagers();
        TrustManager[] b = tmfClient.getTrustManagers();

        // concatenating...
        TrustManager[] result = new TrustManager[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static void main(String[] args) throws Exception {
        PQProxyProperties props = new PQProxyProperties(mainDirectory);

        ClientKey myKey = props.targetClientKey();

        SSLFactory targetSslFactory = SSLFactory.builder()
                .withIdentityMaterial(myKey.key(), myKey.password(), myKey.certificateChain())
                .withProtocols("TLSv1.3")
                .withTrustMaterial(props.targetCaTrustStore())
                .withSecureRandom(SecureRandom.getInstanceStrong())
                .withCiphers("TLS_AES_256_GCM_SHA384")
                .build();

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(loadKeyManager(props), loadAllTrustManagers(props), SecureRandom.getInstanceStrong());

        SourceWsServer sourceWsServer = new SourceWsServer(ctx, props.sourcePort(), (WebSocket sourceClientWs)->{
            class WrappedTargetWsClient {
                TargetWsClient value = null;
            }
            WrappedTargetWsClient wrappedTargetWsClient = new WrappedTargetWsClient();
                // ^^^ value is initialized below; we need to define "targetClient" here due to visibility scope issues
            WsSink sourceSink = new WsSink() {
                @Override
                public void open() {
                    try {
                        wrappedTargetWsClient.value.connectBlockingAndRunAsync();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void consumeMessage(String s) {
                    try {
                        wrappedTargetWsClient.value.wsClient().send(s);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void consumeMessage(ByteBuffer blob) {
                    try {
                        wrappedTargetWsClient.value.wsClient().send(blob);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void closeGracefully(String details) {
                    try {
                        if (!wrappedTargetWsClient.value.wsClient().isClosed())
                            wrappedTargetWsClient.value.wsClient().close();
                    } catch (Exception e) {
                    }
                }

                @Override
                public void closeWithException(Exception e) {
                    try {
                        if (!wrappedTargetWsClient.value.wsClient().isClosed())
                            wrappedTargetWsClient.value.wsClient().close();
                    } catch (Exception ex) {
                    }
                }
            };
            WsSink replySink = new WsSink() {
                @Override
                public void open() {

                }

                @Override
                public void consumeMessage(String s) {
                    sourceClientWs.send(s); // send reply (from the target) back to the source
                }

                @Override
                public void consumeMessage(ByteBuffer blob) {
                    sourceClientWs.send(blob); // send reply (from the target) back to the source
                }

                @Override
                public void closeGracefully(String details) {
                    if (!sourceClientWs.isClosed())
                        sourceClientWs.close();
                }

                @Override
                public void closeWithException(Exception e) {
                    if (!sourceClientWs.isClosed())
                        sourceClientWs.close();
                }
            };

            wrappedTargetWsClient.value = new TargetWsClient(targetSslFactory, props.targetUri(), replySink);


            return sourceSink;
        });

        sourceWsServer.start();

    }


}
