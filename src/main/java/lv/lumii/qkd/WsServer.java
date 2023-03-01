package lv.lumii.qkd;

import org.bouncycastle.pqc.InjectablePQC;
import org.cactoos.Scalar;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Synced;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketServerFactory;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;

public class WsServer implements Server {

    private Scalar<WebSocketServer> wsserver;

    public WsServer(SSLContext sslContext, int port) {
        System.out.println("New WsServer");
        this.wsserver = new Synced<>(new Sticky<>(() -> newConnection(sslContext, port) ));
    }

    private WebSocketServer newConnection(SSLContext sslContext, int port) throws Exception {

        System.out.println("QKD User2 is starting...");

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

        WebSocketServerFactory wsf = new DefaultSSLWebSocketServerFactory(sslContext);
        for (String s: sslContext.getSocketFactory().getDefaultCipherSuites()) {
            System.out.println("CIPHER "+s);
        }

        wssrv.setWebSocketFactory(wsf);
        wssrv.setConnectionLostTimeout(20);
        wssrv.start();
        System.out.println("Server started and ready.");

        return wssrv;
    }

    @Override
    public void start() throws Exception {
        this.wsserver.value(); // init the value, starts the server automatically
    }

}
