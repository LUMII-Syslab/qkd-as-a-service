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

public class DLL_WsClientAPI {

    private static Map<Long, WsClient> wsClientMap = new HashMap<>();
    private static Map<Long, DLL_WsSink> wsSinkMap = new HashMap<>();

    private static final Object mapSem = new Object();

    private static long newHandle() {
        long h;
        do {
            h = Math.round(Math.random() * Long.MAX_VALUE);
        } while (wsSinkMap.containsKey(h));
        return h;
    }

    @CEntryPoint(name = "qaas_connect_to_wss_server")
    public static CCharPointer qaas_connect_to_wss_server(IsolateThread thread, CLongPointer handle) {

        try {
            long h;
            WsClient wsClient;
            synchronized (mapSem) {
                h = newHandle();
                DLL_WsSink wsSink = new DLL_WsSink();
                wsClient = new WsClient(DLL_Common.props.user1SslFactory(), DLL_Common.props.user2Uri(), wsSink, "Some QAAS wss client");
                wsSinkMap.put(h, wsSink);
                wsClientMap.put(h, wsClient);
            }
            handle.write(h);
            wsClient.connectBlockingAndRunAsync();

            new Timer((ms) -> {
                if (ms >= 10000)
                    return true; // stop waiting
                try {
                    if (wsClient.wsClient().isOpen())
                        return true; // ws opened; stop waiting
                } catch (Exception e) {
                }
                return false; // continue waiting
            }).startAndWait();

            if (wsClient.wsClient().isOpen())
                return DLL_Common.NULL_BUFFER; // no error
            else
                throw new Exception("Could not connect to the server for too long.");
        } catch (Exception e) {
            handle.write(0);
            return DLL_Common.toCCharPointer("{\"error\":\"Could not connect to the server: " + e.getMessage() + "\"}");
        }
    }

    @CEntryPoint(name = "qaas_receive_string_from_wss_server")
    public static CCharPointer qaas_receive_string_from_wss_server(IsolateThread thread, long handle, long waitMs, CCharPointerPointer sPtr) {
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

    @CEntryPoint(name = "qaas_receive_binary_from_wss_server")
    public static synchronized CCharPointer qaas_receive_binary_from_wss_server(IsolateThread thread, long handle, long waitMs, CCharPointerPointer bufPtr, CIntPointer len) {
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

    @CEntryPoint(name = "qaas_send_string_to_wss_server")
    public static synchronized CCharPointer qaas_send_string_to_wss_server(IsolateThread thread, long handle, final CCharPointer sPtr) {
        WsClient wsClient;
        synchronized (mapSem) {
            wsClient = wsClientMap.get(handle);
            if (wsClient == null)
                return DLL_Common.toCCharPointer("{\"error\":\"Client not connected.\"}");
        }

        new Timer((ms) -> {
            if (ms >= 10000)
                return true; // stop waiting
            try {
                if (wsClient.wsClient().isOpen())
                    return true; // ws opened; stop waiting
            } catch (Exception e) {
            }
            return false; // continue waiting
        }).startAndWait();

        try {
            String s = DLL_Common.stringFromCCharPointer(sPtr);
            wsClient.wsClient().send(s);
            return DLL_Common.NULL_BUFFER; // empty string
        } catch (Exception e) {
            return DLL_Common.toCCharPointer("{\"error\":\"Could not send string data to web socket.\"}");
        }
    }

    @CEntryPoint(name = "qaas_send_binary_to_wss_server")
    public static synchronized CCharPointer qaas_send_binary_to_wss_server(IsolateThread thread, long handle, final CCharPointer bytes, int len) {
        WsClient wsClient;
        synchronized (mapSem) {
            wsClient = wsClientMap.get(handle);
            if (wsClient == null)
                return DLL_Common.toCCharPointer("{\"error\":\"Client not connected.\"}");
        }

        new Timer((ms) -> {
            if (ms >= 10000)
                return true; // stop waiting
            try {
                if (wsClient.wsClient().isOpen())
                    return true; // ws opened; stop waiting
            } catch (Exception e) {
            }
            return false; // continue waiting
        }).startAndWait();

        try {
            byte[] bb = DLL_Common.byteArrayFromCCharPointer(bytes, len);
            wsClient.wsClient().send(bb);
            return DLL_Common.NULL_BUFFER; // empty string
        } catch (Exception e) {
            return DLL_Common.toCCharPointer("{\"error\":\"Could not send binary data to web socket.\"}");
        }

    }

    // must be called in the end to free client memory
    @CEntryPoint(name = "qaas_disconnect_from_wss_server")
    public static synchronized CCharPointer qaas_disconnect_from_wss_server(IsolateThread thread, long handle) {
        WsClient wsClient;
        synchronized (mapSem) {
            wsClient = wsClientMap.remove(handle);
            wsSinkMap.remove(handle);
        }
        if (wsClient == null) {
            return DLL_Common.toCCharPointer("{\"error\":\"Already disconnected.\"}");
        } else {
            try {
                wsClient.wsClient().close();
            } catch (Exception e) {
            }
        }
        return DLL_Common.NULL_BUFFER; // no error
    }

    /* for testing purposes:
    @CEntryPoint(name = "qaas_user1")
    public static synchronized CCharPointer qaas_user1(IsolateThread thread) {
        try {
            long ms1 = System.currentTimeMillis();
            WsClient wsClient = new WsClient(DLL_Common.props.user1SslFactory(), DLL_Common.props.user2Uri(), () -> "Hi, I am User1!", (user2str) -> {
                long ms2 = System.currentTimeMillis();
                System.out.println("User2 replied with: " + user2str + " time=" + (ms2 - ms1));
            }, (ex) -> {
                System.out.println("User 2 error: " + ex);
            }, "User1 as a client");
            wsClient.connectBlockingAndRunAsync();
            return DLL_Common.NULL_BUFFER; // no error
        } catch (Exception e) {
            return DLL_Common.toCCharPointer("{\"error\":\"Could not connect to the User 2 server: " + e.getMessage() + "\"}");
        }
    }
    */

}
