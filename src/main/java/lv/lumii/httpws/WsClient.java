package lv.lumii.httpws;

import nl.altindag.ssl.SSLFactory;
import org.cactoos.Scalar;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Synced;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.diogonunes.jcolor.Ansi.colorize;
import static com.diogonunes.jcolor.Attribute.GREEN_TEXT;

public class WsClient {

    public interface RequestDataFactory {
        byte[] data() throws Exception;
    }

    public interface RequestStringFactory {
        String data() throws Exception;
    }

    public interface ResponseDataTarget {
        void consume(byte[] data) throws Exception;
    }

    public interface ResponseStringTarget {
        void consume(String data) throws Exception;
    }

    public interface ErrorTarget {
        void consumeError(Exception e);
    }
    private Scalar<WebSocketClient> wsClient;
    private WsSink replySink;

    /**
     *
     * @param sslFactory the optional SSLFactory to use when a TLS web socket is needed
     * @param targetUri the target URI
     * @param fRequest the function that creates binary data to be sent as a request
     * @param fResponse the function that handles the response data;
     *                  after the request, exactly one of fResponse and fError will be called;
     *                  however, if an exception occurs during fResponse, fError is also called
     * @param fError the function that handle the error;
     *               after the request, exactly one of fResponse and fError will be called;
     *               however, if an exception occurs during fResponse, fError is also called
     * @param description the description of this WsClient
     */
    public WsClient(Optional<SSLFactory> sslFactory, URI targetUri, RequestDataFactory fRequest, ResponseDataTarget fResponse, ErrorTarget fError, String description) {
        this(sslFactory, targetUri, new WsSink() {
            private boolean hasBeenSent = false;
            private boolean hasBeenConsumed = false;
            private WebSocket ws = null;
            @Override
            public void open(WebSocket ws) {
                this.ws = ws;
                try {
                    ws.send(fRequest.data());
                    hasBeenSent = true;
                } catch (Exception e) {
                    throw new RuntimeException("Could not send data", e); // should close the web socket
                }
            }

            @Override
            public void consumeMessage(String s) {

            }

            @Override
            public void consumeMessage(ByteBuffer blob) {
                try {
                    fResponse.consume(blob.array());
                }
                catch (Exception e) {
                    fError.consumeError(e);
                }
                hasBeenConsumed = true;
                assert ws != null; // this.ws must have been initialized on open
                ws.close();
            }

            @Override
            public void closeGracefully(String details) {
                if (!hasBeenSent)
                    fError.consumeError(new Exception("No data had been sent before the connection closed."));
                else if (!hasBeenConsumed)
                    fError.consumeError(new Exception("No data received before the connection closed."));
            }

            @Override
            public void closeWithException(Exception e) {
                if (!hasBeenConsumed)
                    fError.consumeError(e);
                // do not send error, since the response data have been already consumed; assume that everything was OK
            }
        }, description);
    }

    /**
     *
     * @param sslFactory the optional SSLFactory to use when a TLS web socket is needed
     * @param targetUri the target URI
     * @param fRequest the function that creates binary data to be sent as a request
     * @param fResponse the function that handles the response data;
     *                  after the request, exactly one of fResponse and fError will be called;
     *                  however, if an exception occurs during fResponse, fError is also called
     * @param fError the function that handle the error;
     *               after the request, exactly one of fResponse and fError will be called;
     *               however, if an exception occurs during fResponse, fError is also called
     * @param description the description of this WsClient
     */
    public WsClient(Optional<SSLFactory> sslFactory, URI targetUri, RequestStringFactory fRequest, ResponseStringTarget fResponse, ErrorTarget fError, String description) {
        this(sslFactory, targetUri, new WsSink() {
            private boolean hasBeenSent = false;
            private boolean hasBeenConsumed = false;
            private WebSocket ws = null;
            @Override
            public void open(WebSocket ws) {
                this.ws = ws;
                try {
                    ws.send(fRequest.data());
                    hasBeenSent = true;
                } catch (Exception e) {
                    throw new RuntimeException("Could not send data", e); // should close the web socket
                }
            }

            @Override
            public void consumeMessage(String s) {
                try {
                    fResponse.consume(s);
                }
                catch (Exception e) {
                    fError.consumeError(e);
                }
                hasBeenConsumed = true;
                assert ws != null; // this.ws must have been initialized on open
                ws.close();
            }

            @Override
            public void consumeMessage(ByteBuffer blob) {

            }

            @Override
            public void closeGracefully(String details) {
                if (hasBeenConsumed)
                    return;
                if (!hasBeenSent) {
                    fError.consumeError(new Exception("No data had been sent before the connection closed to " + targetUri));
                    hasBeenConsumed = true;
                } else if (!hasBeenConsumed) {
                    fError.consumeError(new Exception("No data received before the connection closed to " + targetUri));
                    hasBeenConsumed = true;
                }
            }

            @Override
            public void closeWithException(Exception e) {
                if (hasBeenConsumed)
                    return;
                if (!hasBeenSent) {
                    fError.consumeError(new Exception(e.getMessage()+", no data sent to url=" + targetUri));
                    hasBeenConsumed = true;
                } else if (!hasBeenConsumed) {
                    fError.consumeError(new Exception(e.getMessage()+", no data received from url=" + targetUri));
                    hasBeenConsumed = true;
                }
            }
        }, description);
    }

    public WsClient(Optional<SSLFactory> sslFactory, URI targetUri, WsSink replySink, String description) {
        System.out.println(colorize(description, GREEN_TEXT())+": targetUri="+targetUri+", ssl="+ sslFactory.isPresent());
        this.wsClient = new Sticky<>(() -> newConnection(sslFactory, targetUri) ); // new Synced<>(
        this.replySink = replySink;
    }


    private WebSocketClient newConnection(Optional<SSLFactory> sslFactory, URI targetUri) throws Exception {
        WebSocketClient cln = new WebSocketClient(targetUri) {

            @Override
            protected void onSetSSLParameters(SSLParameters sslParameters) {
                super.onSetSSLParameters(sslParameters);
                List<SNIServerName> list = new LinkedList<>();
                System.out.println("WS CLIENT line 200: setting host name (SNI) to "+targetUri.getHost());
                list.add(new SNIHostName(targetUri.getHost()));
                sslParameters.setServerNames(list);
                sslParameters.setWantClientAuth(true);
                sslParameters.setNeedClientAuth(true);

                sslParameters.setCipherSuites(new String[] {"TLS_AES_256_GCM_SHA384"});
            }

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                System.out.println("WS CLIENT: OPENED");
                replySink.open(this);
            }

            @Override
            public void onMessage(String s) {
                System.out.println("WS CLIENT: TXT MSG received ["+s+"]");
                replySink.consumeMessage(s);
            }

            @Override
            public void onMessage(ByteBuffer blob) {
                System.out.println("WS CLIENT: BYTE MSG received ["+blob.array().length+" bytes]");
                replySink.consumeMessage(blob);
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                System.out.println("WS CLIENT: CLOSED WS ["+s+"]");
                replySink.closeGracefully(s);
            }

            @Override
            public void onError(Exception e) {
                System.out.println("WS CLIENT: Error "+e.getMessage());
                replySink.closeWithException(e);
            }

        };

        cln.setConnectionLostTimeout(20);

        if (sslFactory.isPresent())
            cln.setSocketFactory(sslFactory.get().getSslSocketFactory());

        return cln;
    }

    public void connectBlockingAndRunAsync() {
        try {
            boolean ok = wsClient.value().connectBlocking();
            if (ok) {
                new Thread(()-> {
                    try {
                        //wsClient.value().run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            } else {
                wsClient.value().close(1001, "Could not connect to "+wsClient.value().getRemoteSocketAddress());
            }
        } catch (Exception e) {
        }
    }

    public void connectAndRunAsync() {
        try {
            //wsClient.value().run();
            //wsClient.value().connect();
            new Thread(()-> {
                try {
                    wsClient.value().run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        } catch (Exception e) {
        }
    }

    public WebSocketClient wsClient() throws Exception {
        return wsClient.value();
    }
}
