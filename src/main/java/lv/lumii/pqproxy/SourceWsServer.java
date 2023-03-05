package lv.lumii.pqproxy;

import org.cactoos.Scalar;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Synced;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketServerFactory;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SourceWsServer {

    public interface ClientSourceMessageSinkFactory {
        WsSink createSourceMessageSink(WebSocket client) throws Exception;
    }

    private Scalar<WebSocketServer> wsserver;
    private ClientSourceMessageSinkFactory sinkFactory;

    private Map<WebSocket, WsSink> sourceMessageSinks;

    public SourceWsServer(Optional<SSLContext> sslContext, int port, ClientSourceMessageSinkFactory sinkFactory) {
        System.out.println("New WsServer");
        this.wsserver = new Synced<>(new Sticky<>(() -> newConnection(sslContext, port) ));
        this.sinkFactory = sinkFactory;
        this.sourceMessageSinks = new ConcurrentHashMap<>();
    }

    private WebSocketServer newConnection(Optional<SSLContext> sslContext, int port) throws Exception {

        System.out.println("PQProxy server (listener) is starting...");

        WebSocketServer wssrv = new WebSocketServer(new InetSocketAddress(port)) {

            @Override
            public void onOpen(WebSocket ws, ClientHandshake handshake) {
                WsSink sink = null;
                try {
                    sink = sinkFactory.createSourceMessageSink(ws);
                } catch (Exception e) {
                    ws.close();
                }
                sourceMessageSinks.put(ws, sink);
                System.out.println("Proxy server handshake :" + handshake.toString());
                sink.open();
            }

            @Override
            public void onMessage(WebSocket ws, String msg) {
                System.out.println("Proxy server received:  " + msg + " " + ws.getRemoteSocketAddress());
                sourceMessageSinks.get(ws).consumeMessage(msg);
            }


            @Override
            public void onMessage(WebSocket ws, ByteBuffer msg) {
                System.out.println("Proxy server received:  " + msg.capacity() + " bytes " + ws.getRemoteSocketAddress());
                sourceMessageSinks.get(ws).consumeMessage(msg);
            }
            @Override
            public void onClose(WebSocket ws, int code, String details, boolean byRemoteHost) {
                System.out.println("Proxy server close code=" + code+ " details:"+details);
                sourceMessageSinks.get(ws).closeGracefully(details);
                sourceMessageSinks.remove(ws);
            }

            @Override
            public void onError(WebSocket ws, Exception e) {
                // TODO Auto-generated method stub
                System.out.println("Proxy server receive error " + e);
                sourceMessageSinks.get(ws).closeWithException(e);
                sourceMessageSinks.remove(ws);
            }

            @Override
            public void onStart() {

            }

        };

        if (sslContext.isPresent()) {
            WebSocketServerFactory wsf = new DefaultSSLWebSocketServerFactory(sslContext.get());
            for (String s : sslContext.get().getSocketFactory().getDefaultCipherSuites()) {
                System.out.println("CIPHER " + s);
            }
            wssrv.setWebSocketFactory(wsf); // adding TLS
        }
        wssrv.setConnectionLostTimeout(20);
        wssrv.start();
        System.out.println("Ws server started and ready.");

        return wssrv;
    }

    public void start() throws Exception {
        this.wsserver.value(); // init the value, starts the server automatically
    }

    public WebSocketServer wsServer() throws Exception {
        return this.wsserver.value();
    }


}
