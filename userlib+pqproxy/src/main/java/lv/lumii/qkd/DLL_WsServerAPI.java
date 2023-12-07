package lv.lumii.qkd;


import lv.lumii.httpws.WsServer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.java_websocket.WebSocket;

import javax.net.ssl.SSLContext;
import java.nio.channels.ClosedChannelException;
import java.util.*;

public class DLL_WsServerAPI {

    private static final Object mapSem = new Object();
    private static final Map<Long, WebSocket> wsClientMap = new HashMap<>();
    private static final Map<Long, DLL_WsSink> wsSinkMap = new HashMap<>();
    private static final List<Long> newClients = new LinkedList<>();
    private static WsServer wsServer = null; // will be assigned on start

    private static long newHandle() {
        long h;
        do {
            h = Math.round(Math.random() * Long.MAX_VALUE) + 1;
        } while (wsSinkMap.containsKey(h));
        return h; // returns a non-existing and non-zero handle
    }

    @CEntryPoint(name = "qaas_start_ws_server")
    public static CCharPointer qaas_start_ws_server(IsolateThread thread) {
        try {
            SSLContext ctx = DLL_Common.props.user2SslContext();

            int port = DLL_Common.props.user2Uri().getPort();

            final WsServer srv;
            synchronized (mapSem) {
                if (wsServer != null)
                    throw new Exception("You have to stop the server before starting it again.");
                srv = new WsServer(Optional.of(ctx), port, (WebSocket client) -> new DLL_WsSink() {

                    @Override
                    public void open(WebSocket ws) {
                        super.open(ws);
                        synchronized (mapSem) {
                            long h = newHandle();
                            wsClientMap.put(h, client);
                            wsSinkMap.put(h, this);
                            newClients.add(h);
                        }
                    }

                });
                wsServer = srv;
            }
            srv.start();
            new Timer((ms) -> {
                if (srv.isStarted())
                    return true; // interrupt waiting
                return ms > 5000;
            }).startAndWait();

            if (!srv.isStarted())
                throw new Exception("Could not start the server for too long.");
            return DLL_Common.NULL_BUFFER;
        } catch (Exception e) {
            return DLL_Common.toCCharPointer("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @CEntryPoint(name = "qaas_accept_new_ws_client")
    public static long qaas_accept_new_ws_client(IsolateThread thread, long waitMs) {
        try {
            synchronized (mapSem) {
                if (wsServer == null || !wsServer.isStarted()) {
                    System.out.println("Could not accept client: the server not running "+wsServer);
                    return -1;
                }
            }

            new Timer((ms) -> {
                if (ms > waitMs)
                    return true; // stop waiting
                synchronized (mapSem) {
                    if (wsServer == null || !wsServer.isStarted())
                        return true; // stop waiting
                    return !newClients.isEmpty();
                }
            }).startAndWait();

            synchronized (mapSem) {
                if (!newClients.isEmpty()) {
                    long result = newClients.get(0);
                    newClients.remove(0);
                    return result; // will be > 0
                } else {
                    System.out.println("Could not accept client: timeout");
                    return 0; // zero is ok; signals the invoker to wait for another client
                }
            }
        } catch (Exception e) {
            return -2;
        }
    }

    @CEntryPoint(name = "qaas_receive_string_from_wss_client")
    public static CCharPointer qaas_receive_string_from_wss_client(IsolateThread thread, long handle, long waitMs, CCharPointerPointer sPtr) {
        DLL_WsSink wsSink;
        synchronized (mapSem) {
            wsSink = wsSinkMap.get(handle);
            if (wsSink == null)
                return DLL_Common.NULL_BUFFER; // invalid handle; returning no data
        }

        final CCharPointer[] result = new CCharPointer[1];
        result[0] = DLL_Common.NULL_BUFFER;

        new Timer((ms) -> {
            try {
                CCharPointer s = DLL_Common.toCCharPointer(wsSink.nextString());
                sPtr.write(s);
                return true; // stop waiting
            } catch (NoSuchElementException e1) {
                if (ms >= waitMs) {
                    // just return NULL_BUFFER in bufPtr to denote the timeout
                    return true;
                }
            } catch (ClosedChannelException e) {
                result[0] = DLL_Common.toCCharPointer("{\"error\":\"Channel closed. " + e.getMessage() + "\"}");
                return true;
            }
            return false; // continue waiting
        }).startAndWait();

        return result[0];
    }

    @CEntryPoint(name = "qaas_receive_binary_from_wss_client")
    public static CCharPointer qaas_receive_binary_from_wss_client(IsolateThread thread, long handle, long waitMs, CCharPointerPointer bufPtr, CIntPointer len) {
        DLL_WsSink wsSink;

        synchronized (mapSem) {
            wsSink = wsSinkMap.get(handle);
            if (wsSink == null)
                return DLL_Common.NULL_BUFFER; // invalid handle; returning no data
        }

        bufPtr.write(DLL_Common.NULL_BUFFER);
        len.write(0); // will be replaced by an actual length of received data

        final CCharPointer[] result = new CCharPointer[1];
        result[0] = DLL_Common.NULL_BUFFER;

        new Timer((ms) -> {
            try {
                byte[] bytes = wsSink.nextBinary();
                CCharPointer buf = DLL_Common.toCCharPointer(bytes);
                bufPtr.write(buf);
                len.write(bytes.length);
                return true; // stop waiting
            } catch (NoSuchElementException e1) {
                if (ms >= waitMs) {
                    // just return NULL_BUFFER in bufPtr to denote the timeout
                    return true;
                }
            } catch (ClosedChannelException e) {
                result[0] = DLL_Common.toCCharPointer("{\"error\":\"Channel closed. " + e.getMessage() + "\"}");
                return true;
            }
            return false; // continue waiting
        }).startAndWait();

        return result[0];
    }

    @CEntryPoint(name = "qaas_send_string_to_wss_client")
    public static CCharPointer qaas_send_string_to_wss_client(IsolateThread thread, long handle, final CCharPointer sPtr) {
        WebSocket wsClient;

        synchronized (mapSem) {
            wsClient = wsClientMap.get(handle);
            if (wsClient == null)
                return DLL_Common.toCCharPointer("{\"error\":\"Client not connected.\"}");
        }

        try {
            String s = DLL_Common.stringFromCCharPointer(sPtr);
            wsClient.send(s);
            return DLL_Common.NULL_BUFFER; // empty string
        } catch (Exception e) {
            return DLL_Common.toCCharPointer("{\"error\":\"Could not send string data to web socket.\"}");
        }
    }

    @CEntryPoint(name = "qaas_send_binary_to_wss_client")
    public static CCharPointer qaas_send_binary_to_wss_client(IsolateThread thread, long handle, final CCharPointer bytes, int len) {
        WebSocket wsClient;

        synchronized (mapSem) {
            wsClient = wsClientMap.get(handle);
            if (wsClient == null)
                return DLL_Common.toCCharPointer("{\"error\":\"Client not connected.\"}");
        }

        try {
            byte[] bb = DLL_Common.byteArrayFromCCharPointer(bytes, len);
            wsClient.send(bb);
            return DLL_Common.NULL_BUFFER; // empty string
        } catch (Exception e) {
            return DLL_Common.toCCharPointer("{\"error\":\"Could not send binary data to web socket.\"}");
        }

    }

    @CEntryPoint(name = "qaas_disconnect_wss_client")
    public static CCharPointer qaas_disconnect_wss_client(IsolateThread thread, long handle) {
        WebSocket wsClient;

        synchronized (mapSem) {
            wsClient = wsClientMap.get(handle);
            if (wsClient == null)
                return DLL_Common.toCCharPointer("{\"error\":\"Client not connected.\"}");
            wsClientMap.remove(handle);
            wsSinkMap.remove(handle);
        }

        try {
            wsClient.close();
        } catch (Exception e) {
        }

        return DLL_Common.NULL_BUFFER; // empty string
    }

    @CEntryPoint(name = "qaas_stop_ws_server")
    public static CCharPointer qaas_stop_ws_server(IsolateThread thread) {
        try {
            synchronized (mapSem) {
                if (wsServer == null || !wsServer.isStarted())
                    throw new Exception("The server is not running");
                wsServer.stop();
            }

            new Timer((ms) -> {
                synchronized (mapSem) {
                    if (wsServer == null || !wsServer.isStarted())
                        return true; // interrupt waiting
                }
                return ms > 5000; // interrupt waiting
            }).startAndWait();

            synchronized (mapSem) {
                if (wsServer != null && wsServer.isStarted()) {
                    wsServer = null;
                    throw new Exception("Could not stop the server for too long");
                }
                wsServer = null;
            }
            return DLL_Common.NULL_BUFFER;
        } catch (Exception e) {
            wsServer = null;
            return DLL_Common.toCCharPointer("{\"error\":\"Error while stopping the server: " + e.getMessage() + "\"}");
        }
    }

    /* for testing purposes:
    @CEntryPoint(name = "qaas_user2")
    public static synchronized CCharPointer qaas_user2(IsolateThread thread) {
        try {
            SSLContext ctx = DLL_Common.props.user2SslContext();

            int port = DLL_Common.props.user2Uri().getPort();

            System.out.println("User2 server port=" + port);
            WsServer srv = new WsServer(Optional.of(ctx), port, (WebSocket client) -> new WsSink() {

                @Override
                public void open(WebSocket ws) {
                    System.out.println("User 1: connection opened");
                    client.send("Connected: I am User2!");
                }

                @Override
                public void consumeMessage(String s) {
                    System.out.println("From User1: " + s);
                    client.send("User 2 reply for: [" + s + "]");
                }

                @Override
                public void consumeMessage(ByteBuffer blob) {
                    System.out.println("From User1: binary data of " + blob.array().length + " bytes");
                }

                @Override
                public void closeGracefully(String details) {
                    System.out.println("User2: connection closed");
                }

                @Override
                public void closeWithException(Exception e) {
                    System.out.println("User2: connection exception");
                }
            });

            srv.start();
            return DLL_Common.NULL_BUFFER;
        } catch (Exception e) {
            return DLL_Common.toCCharPointer("{\"error\":\"Could not start the User 2 server: " + e.getMessage() + "\"}");
        }
    }
    */

}
