package lv.lumii.pqproxy;

import nl.altindag.ssl.SSLFactory;
import org.cactoos.Scalar;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Synced;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class TargetWsClient {

    private Scalar<WebSocketClient> wsClient;
    private WsSink replySink;
    public TargetWsClient(SSLFactory sslFactory, URI targetUri, WsSink replySink) {
        this.wsClient = new Synced<>(new Sticky<>(() -> newConnection(sslFactory, targetUri) ));
        this.replySink = replySink;
    }

    private WebSocketClient newConnection(SSLFactory sslFactory, URI targetUri) throws Exception {
        WebSocketClient cln = new WebSocketClient(targetUri) {

            @Override
            protected void onSetSSLParameters(SSLParameters sslParameters) {
                super.onSetSSLParameters(sslParameters);
                List<SNIServerName> list = new LinkedList<>();
                System.out.println("setting host name (SNI) to "+targetUri.getHost());
                list.add(new SNIHostName(targetUri.getHost()));
                sslParameters.setServerNames(list);
                sslParameters.setWantClientAuth(true);
                sslParameters.setNeedClientAuth(true);
                sslParameters.setCipherSuites(new String[] {"TLS_AES_256_GCM_SHA384"});
            }

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                System.out.println("OPENED WS");
                replySink.open();
            }

            @Override
            public void onMessage(String s) {
                System.out.println("TXT MSG WS: ["+s+"]");
                replySink.consumeMessage(s);
            }

            @Override
            public void onMessage(ByteBuffer blob) {
                System.out.println("BYTE MSG WS: ["+blob.array().length+" bytes]");
                replySink.consumeMessage(blob);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                System.out.println("CLOSED WS ["+s+"]");
                replySink.closeGracefully(s);
            }

            @Override
            public void onError(Exception e) {
                System.out.println("Error "+e.getMessage());
                replySink.closeWithException(e);
            }

        };

        cln.setConnectionLostTimeout(20);
        cln.setSocketFactory(sslFactory.getSslSocketFactory());

        return cln;
    }

    public void connectBlockingAndRunAsync() {
        try {
            boolean ok = wsClient.value().connectBlocking();
            if (ok) {
                new Thread(()-> {
                    try {
                        wsClient.value().run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        } catch (Exception e) {
        }
    }

    public WebSocketClient wsClient() throws Exception {
        return wsClient.value();
    }
}
