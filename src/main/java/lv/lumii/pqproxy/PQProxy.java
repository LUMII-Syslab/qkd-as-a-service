package lv.lumii.pqproxy;

import lv.lumii.httpws.*;
import lv.lumii.pqc.InjectablePQC;
import org.bouncycastle.tls.injection.kems.InjectedKEMs;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Optional;

public class PQProxy {


    public static Logger logger; // static initialization

    public static String mainExecutable;
    public static String mainDirectory;

    static {

        InjectablePQC.inject(InjectedKEMs.InjectionOrder.AFTER_DEFAULT);
            // ^^^ makes BouncyCastlePQCProvider the first and BouncyCastleJsseProvider the second

        File f = new File(WsServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        mainExecutable = f.getAbsolutePath();
        mainDirectory = f.getParent();

        // Fix for debug purposes when qkd-client is launched from the IDE:
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/main")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length() - "/build/classes/java/main".length());
            mainExecutable = "java";
        }

        String logFileName = mainDirectory + File.separator + "pqproxy.log";
        System.setProperty("org.slf4j.simpleLogger.logFile", logFileName);
        logger = LoggerFactory.getLogger(PQProxy.class);

        Provider tlsProvider = null;
        try {
            tlsProvider = SSLContext.getInstance("TLS").getProvider();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        logger.info("PQProxy is using TLS provider: " + tlsProvider.getName()); // BCJSSE

    }
/*
    private static KeyManager[] loadKeyManager(PQProxyProperties props) throws Exception {
        KeyManagerFactory kmfSourceServer = KeyManagerFactory.getInstance("SunX509");
        ServerKey k = props.sourceServerKey();
        kmfSourceServer.init(k.keyStore(), k.password());
        return kmfSourceServer.getKeyManagers();
    }

    private static TrustManager[] loadAllTrustManagers(PQProxyProperties props) throws Exception {
        TrustManagerFactory tmfServer, tmfClient;
        tmfServer = TrustManagerFactory.getInstance("SunX509");
        tmfServer.init(props.sourceCaTrustStore());
        tmfClient = TrustManagerFactory.getInstance("SunX509");
        tmfClient.init(props.targetCaTrustStore());

        TrustManager[] a = tmfServer.getTrustManagers();
        TrustManager[] b = tmfClient.getTrustManagers();

        // concatenating...
        TrustManager[] result = new TrustManager[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
*/

    public static SelectableChannel inputStreamToSelectableChannel(InputStream is) throws Exception {
        Pipe pipe = Pipe.open();
        Thread pipeThread = new Thread(() -> {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                while (is.read(buffer.array()) != -1) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        pipe.sink().write(buffer);
                    }
                    buffer.clear();
                }
                pipe.sink().close();
            } catch (Exception e) {
                // handle exception
                e.printStackTrace();
            }
        });
        pipeThread.start();
        return pipe.source();
    }
    public static void main(String[] args) throws Exception {
        PQProxyProperties props;

        if (args.length >= 2 && "-f".equals(args[0])) {
            // -f filename.properties
            File f = new File(args[1]);
            props = new PQProxyProperties(f.getParent(), f);
        } else {
            // -p prop1=value1 -p prop2=value2...
            ArrayList<String> nameValues = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                if ("-p".equals(args[i]) && i + 1 < args.length) {
                    nameValues.add(args[i + 1]);
                    i++;
                }
            }
            if (nameValues.isEmpty())
                props = new PQProxyProperties(mainDirectory);
            else
                props = new PQProxyProperties(mainDirectory, nameValues);
        }

        Optional<SSLContext> ctx = props.sourceServerSslContext();

        if (props.isTargetWebSocket()) {
            WsServer wsServer = new WsServer(ctx, props.sourcePort(), (WebSocket sourceClientWs) -> {
                class WrappedTargetWsClient {
                    WsClient value = null;
                }
                WrappedTargetWsClient wrappedTargetWsClient = new WrappedTargetWsClient();
                // ^^^ value is initialized below, after requestSink and replySink are defined;
                //     we need to wrap the target WsClient here due to visibility scope issues
                WsSink sourceSink = new WsSink() {
                    boolean isOpen = false;

                    @Override
                    public void open(WebSocket ws) {
                        try {
                            wrappedTargetWsClient.value.connectBlockingAndRunAsync();
                            this.isOpen = true;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void consumeMessage(String s) {
                        try {
                            wrappedTargetWsClient.value.wsClient().send(s);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void consumeMessage(ByteBuffer blob) {
                        try {
                            wrappedTargetWsClient.value.wsClient().send(blob);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void closeGracefully(String details) {
                        try {
                            if (!isOpen) {
                                closeWithException(new Exception("Handshake problem"));
                                return;
                            }
                            System.out.println("PROXY CLOSE");
                            isOpen = false;
                            if (!wrappedTargetWsClient.value.wsClient().isClosed())
                                wrappedTargetWsClient.value.wsClient().close();
                        } catch (Exception e) {
                        }
                    }

                    @Override
                    public void closeWithException(Exception e) {
                        try {
                            isOpen = false;
                            System.out.println("PROXY EXCEPTION " + e);
                            if (!wrappedTargetWsClient.value.wsClient().isClosed())
                                wrappedTargetWsClient.value.wsClient().close();
                        } catch (Exception ex) {
                        }
                    }
                };
                WsSink replySink = new WsSink() {
                    @Override
                    public void open(WebSocket ws) {
                        System.out.println("REPLY SINK OPEN OK");
                    }

                    @Override
                    public void consumeMessage(String s) {
                        sourceClientWs.send(s); // send reply (from the target) back to the source
                    }

                    @Override
                    public void consumeMessage(ByteBuffer blob) {
                        sourceClientWs.send(blob); // send reply (from the target) back to the source
                    }

                    @Override
                    public void closeGracefully(String details) {
                        if (!sourceClientWs.isClosed())
                            sourceClientWs.close();
                    }

                    @Override
                    public void closeWithException(Exception e) {
                        if (!sourceClientWs.isClosed())
                            sourceClientWs.close();
                    }
                };

                wrappedTargetWsClient.value = new WsClient(props.targetSslFactory(), props.targetUri(), replySink);


                return sourceSink;
            });
            wsServer.start();
        }
        else {
            RawTlsServer tlsServer = new RawTlsServer(ctx, props.sourcePort(), (sourceSocket) -> {

                Socket targetSocket;
                if (props.isTargetTls()) {
                    SSLSocketFactory factory = props.targetSslFactory().get().getSslSocketFactory();
                    SSLSocket sslTargetSocket =
                            (SSLSocket) factory.createSocket(props.targetUri().getHost(), props.targetUri().getPort());
                    sslTargetSocket.startHandshake();
                    targetSocket = sslTargetSocket;
                }
                else {
                    targetSocket = new Socket(props.targetUri().getHost(), props.targetUri().getPort());
                }

                SocketChannel sourceChannel = sourceSocket.getChannel();
                SocketChannel targetChannel = targetSocket.getChannel();

                sourceChannel = SocketChannel.open();
                sourceChannel.configureBlocking(false);
                sourceChannel.connect(sourceSocket.getRemoteSocketAddress());
                //while (!sourceChannel.finishConnect()) {
                    // Wait for the SocketChannel to finish connecting
                //}

                targetChannel = SocketChannel.open();
                targetChannel.configureBlocking(false);
                targetChannel.connect(targetSocket.getRemoteSocketAddress());
                //while (!targetChannel.finishConnect()) {
                    // Wait for the SocketChannel to finish connecting
                //}


                Pipe pipe = Pipe.open();

                Selector selector = SelectorProvider.provider().openSelector();
                SelectableChannel[] channels = {pipe.source(), sourceChannel, targetChannel};
                int[] ops = {SelectionKey.OP_READ, SelectionKey.OP_READ, SelectionKey.OP_READ};
                SelectionKey[] keys = new SelectionKey[3];

                for (int i = 0; i < channels.length; i++) {
                    channels[i].configureBlocking(false);
                    keys[i] = channels[i].register(selector, ops[i]);
                }

                int tries = 0;

                for(;;) {
                    //selector.select();

                    int readyChannels = selector.selectNow();
                    if (readyChannels == 0) {
                        if (tries > 5000)
                            break; // Waited too long for data. Will close both sockets.

                        Thread.sleep(1);
                        tries++;
                        continue;
                    }
                    tries = 0; // reset

                    for (SelectionKey key : selector.selectedKeys()) {
                        // If the key is for the pipe's source, read from the pipe and write to the server
                        if (key.channel() == pipe.source()) {
                            Pipe.SourceChannel activeChannel = (Pipe.SourceChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            int bytesRead = activeChannel.read(buffer);
                            if (bytesRead > 0) {
                                buffer.flip();
                                while (buffer.hasRemaining()) {
                                    targetChannel.write(buffer);
                                }
                            }
                        }
                        else if (key.channel() == sourceChannel || key.channel() == targetChannel) {
                            SocketChannel socketChannel = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            int bytesRead = socketChannel.read(buffer);
                            if (bytesRead > 0) {
                                buffer.flip();
                                while (buffer.hasRemaining()) {
                                    pipe.sink().write(buffer);
                                }
                            }
                        }
                    }
                    selector.selectedKeys().clear();
                }

/*
                Selector selector = Selector.open();

                SelectableChannel channel1 = inputStreamToSelectableChannel(sourceSocket.getInputStream());
                SelectableChannel channel2 = inputStreamToSelectableChannel(targetSocket.getInputStream());


                channel1.configureBlocking(false);
                channel2.configureBlocking(false);
                channel1.register(selector, SelectionKey.OP_READ);
                channel2.register(selector, SelectionKey.OP_READ);

*\


                int tries = 0;
                for(;;) {

                    /*int readyChannels = selector.selectNow();
                    if (readyChannels == 0) {
                        Thread.sleep(1);
                        continue;
                    }*\

                    int n = sourceSocket.getInputStream().available() + targetSocket.getInputStream().available();
                    if (n==0) {
                        if (tries > 1000) {
                            sourceSocket.getInputStream().wait();
                        }
                        if (tries > 5000)
                            break; // Waited too long for data. Will close both sockets.

                        Thread.sleep(1);
                        tries++;
                        continue;
                    }
                    tries = 0; // reset

                    n = sourceSocket.getInputStream().available();
                    while (n>0) {
                        byte[] buf = sourceSocket.getInputStream().readNBytes(n);
                        targetSocket.getOutputStream().write(buf);
                        targetSocket.getOutputStream().flush();
                        n = sourceSocket.getInputStream().available();
                    }
                    n = targetSocket.getInputStream().available();
                    while (n>0) {
                        byte[] buf = targetSocket.getInputStream().readNBytes(n);
                        sourceSocket.getOutputStream().write(buf);
                        sourceSocket.getOutputStream().flush();
                        n = targetSocket.getInputStream().available();
                    }


                }*/
                targetSocket.close();
                sourceSocket.close();
            });

            tlsServer.start();

        }

    }


}

