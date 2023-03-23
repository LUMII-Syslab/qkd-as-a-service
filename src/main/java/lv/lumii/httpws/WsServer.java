package lv.lumii.httpws;

import lv.lumii.httpws.WsSink;
import org.cactoos.Scalar;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Synced;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketServerFactory;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.SSLParametersWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class WsServer {

    public interface ClientSourceMessageSinkFactory {
        WsSink createSourceMessageSink(WebSocket client) throws Exception;
    }

    private Scalar<WebSocketServer> wsserver;
    private ClientSourceMessageSinkFactory sinkFactory;

    private Map<WebSocket, WsSink> sourceMessageSinks;

    public WsServer(Optional<SSLContext> sslContext, int port, ClientSourceMessageSinkFactory sinkFactory) {
        System.out.println("New WsServer, port="+port+", ssl="+ sslContext.isPresent());
        this.wsserver = new Synced<>(new Sticky<>(() -> newConnection(sslContext, port) ));
        this.sinkFactory = sinkFactory;
        this.sourceMessageSinks = new ConcurrentHashMap<>();
    }

    private WebSocketServer newConnection(Optional<SSLContext> sslContext, int port) throws Exception {

        System.out.println("WsServer (listener) is starting...");

        WebSocketServer wssrv = new WebSocketServer(new InetSocketAddress(port)) {

            @Override
            public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
                ServerHandshakeBuilder b = super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);

                try {
                    if (sslContext.isPresent()) {
                        Certificate[] certs = conn.getSSLSession().getPeerCertificates();
                        System.out.println("WsServer GOT " + certs.length + " client certs");
                    }
                    else {
                        System.out.println("WsServer NO SSL");
                    }
                } catch (SSLPeerUnverifiedException e) {
                    e.printStackTrace();
                    //throw new RuntimeException(e);
                }
                System.out.println("WsServer received client handshake b="+b);
                return b;
            }
            @Override
            public void onOpen(WebSocket ws, ClientHandshake handshake) {
                try {
                    if (sslContext.isPresent()) {
                        Certificate[] certs = ws.getSSLSession().getPeerCertificates();
                        System.out.println("OPEN WsServer GOT " + certs.length + " client certs");
                    }
                    else {
                        System.out.println("OPEN WsServer NO SSL");
                    }
                } catch (SSLPeerUnverifiedException e) {
                    e.printStackTrace();
                    //throw new RuntimeException(e);
                }

                WsSink sink = null;
                try {
                    sink = sinkFactory.createSourceMessageSink(ws);
                } catch (Exception e) {
                    ws.close();
                }
                sourceMessageSinks.put(ws, sink);
                System.out.println("WsServer handshake :" + handshake.toString());
                sink.open(ws);
            }

            @Override
            public void onMessage(WebSocket ws, String msg) {
                System.out.println("WsServer received:  " + msg + " " + ws.getRemoteSocketAddress());
                try {
                    if (sslContext.isPresent()) {
                        Certificate[] certs = ws.getSSLSession().getPeerCertificates();
                        System.out.println("MSG WsServer GOT " + certs.length + " client certs");
                    }
                    else {
                        System.out.println("MSG WsServer NO SSL");
                    }
                } catch (SSLPeerUnverifiedException e) {
                    e.printStackTrace();
                    //throw new RuntimeException(e);
                }
                sourceMessageSinks.get(ws).consumeMessage(msg);
            }


            @Override
            public void onMessage(WebSocket ws, ByteBuffer msg) {
                System.out.println("WsServer received:  " + msg.capacity() + " bytes " + ws.getRemoteSocketAddress());
                sourceMessageSinks.get(ws).consumeMessage(msg);
            }
            @Override
            public void onClose(WebSocket ws, int code, String details, boolean byRemoteHost) {
                System.out.println("WsServer close code=" + code+ " details:"+details);
                WsSink sink = sourceMessageSinks.remove(ws);
                sink.closeGracefully(details);
            }

            @Override
            public void onError(WebSocket ws, Exception e) {
                System.out.println("Proxy server receive error " + e + " ws="+ws);
                if (ws!=null) {
                    WsSink sink = sourceMessageSinks.remove(ws);
                    if (sink != null)
                        sink.closeWithException(e);
                }
            }

            @Override
            public void onStart() {

            }

        };

        if (sslContext.isPresent()) {
            //SSLParameters sslParameters = new SSLParameters();
            SSLParameters sslParameters = sslContext.get().getDefaultSSLParameters();
            //WebSocketServerFactory wsf = new DefaultSSLWebSocketServerFactory(sslContext.get());

            sslParameters.setWantClientAuth(true);
            sslParameters.setNeedClientAuth(true);
            sslParameters.setCipherSuites(new String[] {"TLS_AES_256_GCM_SHA384"});

            WebSocketServerFactory wsf = new SSLParametersWebSocketServerFactory(sslContext.get(), sslParameters);
            for (String s : sslContext.get().getSocketFactory().getDefaultCipherSuites()) {
                System.out.println("WsServer cipher found: " + s);
            }
            wssrv.setWebSocketFactory(wsf); // adding TLS
        }

        //wssrv.set
        wssrv.setConnectionLostTimeout(20);
        wssrv.start();
        System.out.println("WsServer started and ready.");

        return wssrv;
    }

    public void start() throws Exception {
        this.wsserver.value(); // init the value, starts the server automatically
    }

    public WebSocketServer wsServer() throws Exception {
        return this.wsserver.value();
    }


}
