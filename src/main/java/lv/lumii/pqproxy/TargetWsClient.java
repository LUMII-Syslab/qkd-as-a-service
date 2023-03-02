package lv.lumii.pqproxy;

import nl.altindag.ssl.SSLFactory;
import org.cactoos.Scalar;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Synced;
import org.java_websocket.client.WebSocketClient;

import java.net.URI;

public class WsClient {

    private Scalar<WebSocketClient> wsClient;
    public WsClient(SSLFactory sslFactory, URI targetUri) {
        this.wsClient = new Synced<>(new Sticky<>(() -> newConnection(sslFactory, targetUri) ));
    }

    private WebSocketClient newConnection(SSLFactory sslFactory, URI targetUri) throws Exception {

    }
}
