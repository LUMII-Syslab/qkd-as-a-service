package lv.lumii.pqproxy;

import lv.lumii.keys.ServerKey;
import lv.lumii.keys.ClientKey;
import nl.altindag.ssl.SSLFactory;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

public class PQProxyProperties {

    Logger logger = LoggerFactory.getLogger(PQProxyProperties.class);
    private String mainDirectory;
    private Unchecked<Properties> properties;

    public PQProxyProperties(String mainDirectory) {
        this(mainDirectory, new File(mainDirectory + File.separator + "pqproxy.properties"));
    }

    public PQProxyProperties(String mainDirectory, File f) {
        this.mainDirectory = mainDirectory;
        this.properties = new Unchecked<>(new Sticky<>(
                () -> loadPropertiesFile(f.getAbsolutePath())));
    }

    public PQProxyProperties(String mainDirectory, Collection<String> nameVals) {
        this.mainDirectory = mainDirectory;
        this.properties = new Unchecked<>(new Sticky<>(
                () -> loadPropertiesFromNameValues(nameVals)));
    }
    private Properties loadPropertiesFile(String fileName) {
        Properties p = new Properties();

        try {
            p.load(new BufferedReader(new FileReader(fileName)));
        } catch (IOException e) {
            logger.error("Could not load properties from "+fileName, e);
        }

        return p;
    }

    private Properties loadPropertiesFromNameValues(Collection<String> nameVals) {
        Properties p = new Properties();

        for (String s : nameVals) {
            int i = s.indexOf('=');
            if (i>=0) {
                p.setProperty(s.substring(0,i),s.substring(i+1));
            }
            else
                p.setProperty(s, "true"); // assume the boolean property
        }

        return p;
    }


    private String fileNameProperty(String key, String defaultValue) {
        String fileName = this.properties.value().getProperty(key, defaultValue);
        File f = new File(fileName);
        if (!f.isFile())
            f = new File(mainDirectory+File.separator+fileName);
        return f.getAbsolutePath();
    }

    public URI targetUri() throws Exception {
        String host = properties.value().getProperty("targetUri");
        if (host==null)
            throw new Exception("The target URI has not been specified in properties.");
        return new URI(host);
    }

    public boolean sourceTls() {
        boolean defaultValue = false;
        String s = properties.value().getProperty("sourceTls", defaultValue+"");
        try {
            boolean result = Boolean.parseBoolean(s);
            return result;
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    public int sourcePort() throws Exception {
        int defaultPort = 8000;
        String s = properties.value().getProperty("sourcePort", defaultPort+"");
        try {
            int result = Integer.parseInt(s);
            if (result<=0)
                return defaultPort;
            return result;
        }
        catch (Exception e) {
            return defaultPort;
        }
    }

    public ServerKey sourceServerKey() {
        return new ServerKey(
                fileNameProperty("sourceServerKeyStore", "source-server.keystore"),
                this.properties.value().getProperty("sourceServerKeyStorePassword", "server-keystore-pass"),
                this.properties.value().getProperty("sourceServerKeyAlias", "server")
        );
    }

    public KeyStore sourceCaTrustStore() throws Exception {

        String fileName = fileNameProperty("sourceCaTrustStore", "source-ca.truststore");
        File f = new File(fileName);

        String password = properties.value().getProperty("sourceCaTrustStorePassword", "ca-truststore-pass"); // ca-truststore-pass

        return KeyStore.getInstance(f, password.toCharArray());
    }

    public KeyStore targetCaTrustStore() throws Exception {

        String fileName = fileNameProperty("targetCaTrustStore", "target-ca.truststore");
        File f = new File(fileName);

        String password = properties.value().getProperty("targetCaTrustStorePassword", "ca-truststore-pass"); // ca-truststore-pass

        return KeyStore.getInstance(f, password.toCharArray());
    }

    public ClientKey targetClientKey() {
        return new ClientKey(
                fileNameProperty("targetClientKeyStore", "target-client.keystore"),
                this.properties.value().getProperty("targetClientKeyStorePassword", "client-keystore-pass"),
                this.properties.value().getProperty("targetClientKeyAlias", "client")
        );
    }

    public Optional<SSLContext> sourceServerSslContext() throws Exception {
        if (this.sourceTls()) {
            ServerKey srvKey = this.sourceServerKey();

            KeyManagerFactory kmf;
            KeyStore ks;
            kmf = KeyManagerFactory.getInstance("SunX509");
            ks = srvKey.keyStore();
            kmf.init(ks, srvKey.password());

            TrustManagerFactory tmf;
            KeyStore ts;
            tmf = TrustManagerFactory.getInstance("SunX509");
            ts = this.sourceCaTrustStore();
            tmf.init(ts);

            SSLContext ctx;
            //ctx = SSLContext.getInstance("TLSv1.2");
            ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), SecureRandom.getInstanceStrong());
            return Optional.of(ctx);
        }
        else
            return Optional.empty();
    }

    public boolean isTargetWebSocket() throws Exception {
        String targetProtocol = this.targetUri().getScheme();
        return "ws".equals(targetProtocol) || "wss".equals(targetProtocol);
    }

    public boolean isTargetTls() throws Exception {
        String targetProtocol = this.targetUri().getScheme();
        return "wss".equals(targetProtocol) || "https".equals(targetProtocol);
    }
    public Optional<SSLFactory> targetSslFactory() throws Exception {
        if (this.isTargetTls()) {
            ClientKey myKey = this.targetClientKey();
            return Optional.of(
                    SSLFactory.builder()
                            .withIdentityMaterial(myKey.key(), myKey.password(), myKey.certificateChain())
                            .withProtocols("TLSv1.3")
                            .withTrustMaterial(this.targetCaTrustStore())
                            .withSecureRandom(SecureRandom.getInstanceStrong())
                            .withCiphers("TLS_AES_256_GCM_SHA384")
                            .build());
        }
        else {
            return Optional.empty();
        }
    }
}
