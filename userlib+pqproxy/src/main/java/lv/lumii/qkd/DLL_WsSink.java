package lv.lumii.qkd;

import lv.lumii.httpws.WsSink;
import org.java_websocket.WebSocket;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class DLL_WsSink implements WsSink {

    private final List<String> stringBuf;
    private final List<byte[]> binaryBuf;

    private boolean isOpen;

    public DLL_WsSink() {
        stringBuf = new LinkedList<>();
        binaryBuf = new LinkedList<>();
        isOpen = false;
    }

    @Override
    public synchronized void open(WebSocket ws) {
        this.isOpen = true;
    }

    @Override
    public synchronized void consumeMessage(String s) {
        stringBuf.add(s);
    }

    @Override
    public synchronized void consumeMessage(ByteBuffer blob) {
        binaryBuf.add(blob.array());
    }

    @Override
    public synchronized void closeGracefully(String details) {
        this.isOpen = false;
    }

    @Override
    public synchronized void closeWithException(Exception e) {
        this.isOpen = false;
    }

    public synchronized String nextString() throws NoSuchElementException, ClosedChannelException {
        if (stringBuf.isEmpty()) {
            if (isOpen)
                throw new NoSuchElementException();
            else
                throw new ClosedChannelException();
        }
        else {
            String result = stringBuf.get(0);
            stringBuf.remove(0);
            return result;
        }
    }

    public synchronized byte[] nextBinary() throws NoSuchElementException, ClosedChannelException {
        if (binaryBuf.isEmpty()) {
            if (isOpen)
                throw new NoSuchElementException();
            else
                throw new ClosedChannelException();
        }
        else {
            byte[] result = binaryBuf.get(0);
            binaryBuf.remove(0);
            return result;
        }
    }


}
