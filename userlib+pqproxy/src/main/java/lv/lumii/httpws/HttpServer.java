package lv.lumii.httpws;

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

public class HttpServer implements Runnable, Server {

    private ServerSocket server;
    private HttpRequestFunction requestFunction;

    /**
     * Constructs a ClassServer based on <b>ss</b> and
     * obtains a file's bytecodes using the method <b>getBytes</b>.
     */
    public HttpServer(Optional<SSLContext> sslContext, int port, HttpRequestFunction requestFunction) throws IOException {

        SSLServerSocketFactory ssf = sslContext.get().getServerSocketFactory();// .getServerSocketFactory();
        server = ssf.createServerSocket(port);
        if (sslContext.isPresent()) {
            //SSLParameters sslParameters = new SSLParameters();
            SSLParameters sslParameters = sslContext.get().getDefaultSSLParameters();

            sslParameters.setWantClientAuth(true);
            sslParameters.setNeedClientAuth(true);
            sslParameters.setCipherSuites(new String[]{"TLS_AES_256_GCM_SHA384"});

            if (server instanceof SSLServerSocket)
                ((SSLServerSocket) server).setSSLParameters(new SSLParameters());
        }
        this.requestFunction = requestFunction;
    }

    /**
     * Returns the path to the file obtained from
     * parsing the HTML header.
     */
    private static String getPath(BufferedReader in)
            throws IOException {
        String line = in.readLine();
        String path = "";
        // extract class from GET line
        if (line.startsWith("GET /")) {
            line = line.substring(5, line.length() - 1).trim();
            int index = line.indexOf(' ');
            if (index != -1) {
                path = line.substring(0, index);
            }
        }
        System.err.println("PATH=[" + path + "]");

        // eat the rest of header
        do {
            line = in.readLine();
        } while ((line.length() != 0) &&
                (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));

        if (path.length() != 0) {
            return path;
        } else {
            return "/";
        }
    }

    public void run() {
        Socket socket;

        // accept a connection
        try {
            socket = server.accept();
        } catch (IOException e) {
            System.out.println("Class Server died: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // create a new thread to accept the next connection
        try {
            newListener();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            OutputStream rawOut = socket.getOutputStream();

            try {
                // get path to class file from header
                BufferedReader in =
                        new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));

                HttpRequest.Builder requestBuilder = HttpRequestFactory.createRequestBuilder(in);
                // retrieve bytecodes
                /*byte[] bytecodes = getBytes(path);
                // send bytecodes in response (assumes HTTP/1.0 or later)
                try {
                    out.print("HTTP/1.0 200 OK\r\n");
                    out.print("Content-Length: " + bytecodes.length +
                            "\r\n");
                    out.print("Content-Type: text/html\r\n\r\n");
                    out.flush();
                    rawOut.write(bytecodes);
                    rawOut.flush();
                } catch (IOException ie) {
                    ie.printStackTrace();
                    return;
                }*/

                HttpResponse<byte[]> response = requestFunction.processRequest(socket, requestBuilder);
                rawOut.write(
                        ("HTTP/" + response.version() + " "
                                + response.statusCode() + " " + (response.statusCode() == 200 ? "OK" : "NOT OK"))
                                .getBytes(StandardCharsets.UTF_8));
                Map<String, List<String>> m = response.headers().map();
                for (String k : m.keySet()) {
                    for (String v : m.get(k)) {
                        String s = k + ": " + v + "\r\n";
                        rawOut.write(s.getBytes(StandardCharsets.UTF_8));
                    }
                }
                rawOut.write("\r\n".getBytes(StandardCharsets.UTF_8));
                rawOut.write(response.body());
                rawOut.flush();

            } catch (Exception e) {
                e.printStackTrace();
                // write out error response
                rawOut.write(("HTTP/1.0 400 " + e.getMessage() + "\r\n").getBytes(StandardCharsets.UTF_8));
                rawOut.write("Content-Type: text/html\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                rawOut.flush();
            }

        } catch (IOException ex) {
            // eat exception (could log error to log file, but
            // write out to stdout for now).
            System.out.println("error writing response: " + ex.getMessage());
            ex.printStackTrace();

        } finally {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    private void newListener() throws Exception {
        (new Thread(this)).start();
    }

    @Override
    public void start() throws Exception {
        newListener();
    }

    public interface HttpRequestFunction {
        //void processRequest(Socket socket, String path, Reader request, Writer response) throws Exception;
        HttpResponse<byte[]> processRequest(Socket socket, HttpRequest.Builder requestBuilder) throws Exception;
    }
}