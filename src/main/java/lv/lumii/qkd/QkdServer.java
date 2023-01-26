package lv.lumii.qkd;

import lv.lumii.qrng.ClientBuffer;
import lv.lumii.qrng.QrngProperties;
import nl.altindag.ssl.SSLFactory;
import org.cactoos.Scalar;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.security.SecureRandom;

public class QkdServer {

    public static Logger logger = QkdServer.logger; // one common logger

    //private QkdProperties qkdProperties;
    //private ClientBuffer clientBuffer;

    private int port = 2222;
    private Scalar<WebSocketServer> wsserver;

    public QkdServer(QrngProperties qrngProperties1, ClientBuffer clientBuffer) {
        System.out.println(" New QkdServer");

    }

    private WebSocketServer newConnection() throws Exception {

        System.out.println(" New QKD User2 ");

        //QrngClientToken token = qrngProperties.clientToken();

        //System.out.println("TOKEN "+token.password()+" "+token.certificateChain());

        //TrustManagerFactory trustMgrFact = TrustManagerFactory.getInstance("SunX509");
        //trustMgrFact.init(qrngProperties.trustStore());

        SSLFactory sslf2 = SSLFactory.builder()
                //.withIdentityMaterial(token.key(), token.password(), token.certificateChain())
                //.withNeedClientAuthentication()
                //.withWantClientAuthentication()
                .withProtocols("TLSv1.3")
                //.withTrustMaterial(trustMgrFact)
                .withSecureRandom(SecureRandom.getInstanceStrong())
                .withCiphers("TLS_AES_256_GCM_SHA384")
                .build();



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
        //Start Server functionality
        wssrv.start();
        System.out.println("Server started and ready.");

        return wssrv;
    }

}
