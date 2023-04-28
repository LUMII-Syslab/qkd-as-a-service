package lv.lumii.httpws;

import lv.lumii.pqproxy.PQProxyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class RawTlsServer implements Runnable, Server {
    private static Logger logger = LoggerFactory.getLogger(PQProxyProperties.class);

    private static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private ServerSocket server;
    private NewClientFunction newClientFunction;

    public interface NewClientFunction {
        void serveClient(Socket client) throws Exception;
    }


    public RawTlsServer(Optional<SSLContext> sslContext, int port, NewClientFunction newClientFunction) throws IOException {


        this.newClientFunction = newClientFunction;
        if (sslContext.isPresent()) {
            SSLServerSocketFactory ssf = sslContext.get().getServerSocketFactory();// .getServerSocketFactory();
            this.server = ssf.createServerSocket(port);

            //SSLParameters sslParameters = new SSLParameters();
            SSLParameters sslParameters = sslContext.get().getDefaultSSLParameters();

            sslParameters.setWantClientAuth(true);
            sslParameters.setNeedClientAuth(true);
            sslParameters.setCipherSuites(new String[]{"TLS_AES_256_GCM_SHA384"});

            if (server instanceof SSLServerSocket)
                ((SSLServerSocket) server).setSSLParameters(new SSLParameters());
        }
        else {
            this.server = new ServerSocket(port);
        }
    }


    public void run() {
        Socket socket;

        // accept a connection
        try {
            socket = server.accept();
        } catch (Exception e) {
            logger.error("Server died: " + e.getMessage(), e);
            return;
        }

        // create a new thread to accept the next connection
        try {
            newListener();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            newClientFunction.serveClient(socket);
        } catch (Exception ex) {
            logger.error("error during processing client request: " + ex.getMessage(), ex);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    private void newListener() throws Exception {
        executor.execute(this::run);
        // Simple but thread-consuming variant: (new Thread(this)).start();
    }

    @Override
    public void start() throws Exception {
        newListener();
    }

}