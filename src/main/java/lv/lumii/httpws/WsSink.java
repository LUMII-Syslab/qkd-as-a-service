package lv.lumii.httpws;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ServerHandshake;

import java.nio.ByteBuffer;

public interface WsSink {

    void open(WebSocket ws);
    void consumeMessage(String s);
    void consumeMessage(ByteBuffer blob);

    void closeGracefully(String details);
    void closeWithException(Exception e);
}
