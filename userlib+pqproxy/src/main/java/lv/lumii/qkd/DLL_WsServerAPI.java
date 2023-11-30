package lv.lumii.qkd;


import lv.lumii.httpws.WsClient;
import lv.lumii.httpws.WsServer;
import lv.lumii.httpws.WsSink;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.word.WordFactory;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DLL_WsServerAPI {


    private static Map<Long, WebSocket> wsClientMap = new HashMap<>();
    private static Map<Long, DLL_WsSink> wsSinkMap = new HashMap<>();
    private static List<Long> newClients = new LinkedList<>();
    private static WsServer wsServer = null; // will be assigned on start

    private static final Object mapSem = new Object();

    private static long newHandle() {
        long h;
        do {
            h = Math.round(Math.random() * Long.MAX_VALUE);
        } while (wsSinkMap.containsKey(h));
        return h;
    }


    @CEntryPoint(name = "qaas_start_ws_server")
    public static CCharPointer qaas_start_ws_server(IsolateThread thread) {
        try {
            SSLContext ctx = DLL_Common.props.user2SslContext();

            int port = DLL_Common.props.user2Uri().getPort();

            synchronized (mapSem) {
                if (wsServer != null)
                    throw new Exception("You have to stop the server before starting it again.");
                wsServer = new WsServer(Optional.of(ctx), port, (WebSocket client) -> new DLL_WsSink() {

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
                wsServer.start();
            }
            new Timer((ms) -> {
                if (wsServer.isStarted())
                    return true; // interrupt waiting
                if (ms > 5000)
                    return true; // interrupt waiting
                return false; // continue waiting
            }).startAndWait();

            if (!wsServer.isStarted())
                throw new Exception("Could not start the server for too long.");
            return DLL_Common.NULL_BUFFER;
        } catch (Exception e) {
            return DLL_Common.toCCharPointer("{\"error\":\"Could start the server: " + e.getMessage() + "\"}");
        }
    }

    @CEntryPoint(name = "qaas_accept_new_ws_client")
    public static long qaas_accept_new_ws_client(IsolateThread thread) {
        try {
            synchronized (mapSem) {
                if (wsServer == null || !wsServer.isStarted()) {
                    System.out.println("Could not accept client: the server not running");
                    return -1;
                }
            }

            new Timer((ms) -> {
                synchronized (mapSem) {
                    if (wsServer == null || !wsServer.isStarted())
                        return true; // stop waiting
                }
                return !newClients.isEmpty();
            }).startAndWait();

            if (!newClients.isEmpty()) {
                long result = newClients.get(0);
                System.out.println("New client accepted: " + result);
                newClients.remove(0);
                return result;
            } else {
                System.out.println("Could not accept client: timeout or the server is not running.");
                return -1;
            }
        } catch (Exception e) {
            return -1;
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
                    result[0] = DLL_Common.toCCharPointer("{\"error\":\"Timeout\"}");
                    return true;
                }
            } catch (ClosedChannelException e) {
                result[0] = DLL_Common.toCCharPointer("{\"error\":\"Channel closed. "+e.getMessage()+"\"}");
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

        len.write(0); // will be replaced by an actual length of received data

        final CCharPointer[] result = new CCharPointer[1];
        result[0] = DLL_Common.NULL_BUFFER;

        new Timer((ms) -> {
            try {
                byte[] bytes = wsSink.nextBinary();
                CCharPointer buf = DLL_Common.toCCharPointer(bytes);
                bufPtr.write(buf);
                len.write(bytes.length);
            } catch (NoSuchElementException e1) {
                if (ms >= waitMs) {
                    result[0] = DLL_Common.toCCharPointer("{\"error\":\"Timeout\"}");
                    return true;
                }
            } catch (ClosedChannelException e) {
                result[0] = DLL_Common.toCCharPointer("{\"error\":\"Channel closed. "+e.getMessage()+"\"}");
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
                wsServer.wsServer().stop();
            }

            new Timer((ms) -> {
                synchronized (mapSem) {
                    if (wsServer == null || !wsServer.isStarted())
                        return true; // interrupt waiting
                }
                if (ms > 5000)
                    return true; // interrupt waiting
                return false; // continue waiting
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
