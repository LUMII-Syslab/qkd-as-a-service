package lv.lumii.test;


import lv.lumii.httpws.HttpServer;

import java.net.URI;
import java.net.http.HttpClient;
import lv.lumii.httpws.WsServer;
import lv.lumii.httpws.WsSink;
import lv.lumii.qkd.QkdProperties;
import org.bouncycastle.pqc.InjectablePQC;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.io.File;
import java.net.Socket;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.*;

public class PQProxyHttpTestServer {

    public static Logger logger; // static initialization

    public static String mainExecutable;
    public static String mainDirectory;

    static {

        InjectablePQC.inject(); // makes BouncyCastlePQCProvider the first and BouncyCastleJsseProvider the second

        File f = new File(PQProxyHttpTestServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        mainExecutable = f.getAbsolutePath();
        mainDirectory = f.getParent();

        // Fix for debug purposes when qkd-client is launched from the IDE:
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/main")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length()-"/build/classes/java/main".length());
            mainExecutable = "java";
        }
        if (mainExecutable.replace('\\', '/').endsWith("/build/classes/java/test")) {
            mainDirectory = mainExecutable.substring(0, mainExecutable.length()-"/build/classes/java/test".length());
            mainExecutable = "java";
        }

        String logFileName = mainDirectory+ File.separator+"wsserver.log";
        System.setProperty("org.slf4j.simpleLogger.logFile", logFileName);
        logger = LoggerFactory.getLogger(PQProxyHttpTestServer.class);

        Provider tlsProvider = null;
        try {
            tlsProvider = SSLContext.getInstance("TLS").getProvider();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        logger.info("Using TLS provider: "+tlsProvider.getName()); // BCJSSE

    }

    public static void main(String[] args) throws Exception {

        QkdProperties qkdProperties = new QkdProperties(mainDirectory);
        SSLContext ctx = qkdProperties.serverSslContext();

        System.out.println("Http test server port="+qkdProperties.port());
        HttpServer srv = new HttpServer(
                Optional.of(ctx),
                qkdProperties.port(),
                (Socket socket, HttpRequest.Builder requestBuilder) -> {

                    HttpClient client;
                    if ("https".equals(qkdProperties.remoteUri().getScheme()))
                        client = HttpClient
                                .newBuilder()
                                .sslContext(qkdProperties.serverSslContext())
                                .build();
                    else
                        client = HttpClient
                                .newBuilder()
                                .build();

                    final HttpRequest request = requestBuilder.build();
                    try {
                        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                        System.out.println("SMART RESPONSE: " + response.body());
                        return response;
                    }catch (Exception e ){
                        e.printStackTrace();

                        String s = "HTTP/1.1 400 " + e.getMessage() + "\r\n" +
                                "Content-Type: text/html\r\n\r\n";
                        return new HttpResponse<byte[]>() {
                            @Override
                            public int statusCode() {
                                return 400;
                            }

                            @Override
                            public HttpRequest request() {
                                return request;
                            }

                            @Override
                            public Optional<HttpResponse<byte[]>> previousResponse() {
                                return Optional.empty();
                            }

                            @Override
                            public HttpHeaders headers() {
                                Map<String, List<String>> m = new HashMap<>();
                                List<String> l = new LinkedList<>();
                                l.add("text/html");
                                m.put("Content-Type", l);
                                return HttpHeaders.of(m, (a,b)->true);
                            }

                            @Override
                            public byte[] body() {
                                return new byte[0];
                            }

                            @Override
                            public Optional<SSLSession> sslSession() {
                                return Optional.empty();
                            }

                            @Override
                            public URI uri() {
                                return null;
                            }

                            @Override
                            public HttpClient.Version version() {
                                return HttpClient.Version.HTTP_1_1;
                            }
                        };
                    }

                });

        srv.start();

    }


}
