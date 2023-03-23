package lv.lumii.qkd;

import lv.lumii.keys.ClientKey;
import lv.lumii.keys.ServerKey;
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
import java.util.Optional;
import java.util.Properties;

public class QkdProperties {

    Logger logger = LoggerFactory.getLogger(QkdProperties.class);
    private String mainDirectory;
    private Unchecked<Properties> properties;

    public QkdProperties(String mainDirectory) {
        System.out.println("MAIN "+mainDirectory);
        this.mainDirectory = mainDirectory;
        this.properties = new Unchecked<>(new Sticky<>(
                () -> loadPropertiesFile(mainDirectory + File.separator + "qkd.properties")));
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

    private String fileNameProperty(String key, String defaultValue) {
        String fileName = this.properties.value().getProperty(key, defaultValue);
        File f = new File(fileName);
        if (!f.isFile())
            f = new File(mainDirectory+File.separator+fileName);
        return f.getAbsolutePath();
    }

    public URI serverUri() throws Exception {
        String uri = properties.value().getProperty("serverUri", "wss://localhost:443");
        return new URI(uri);
    }

    public ServerKey serverKey() {
        return new ServerKey(
                fileNameProperty("serverKeyStore", "server.keystore"),
                this.properties.value().getProperty("serverKeyStorePassword", "server-keystore-pass"),
                this.properties.value().getProperty("serverKeyAlias", "server")
                        //"qkd_user2")
        );
    }

    public KeyStore caTrustStore() throws Exception {

        String fileName = fileNameProperty("caTrustStore", "ca.truststore");
        File f = new File(fileName);

        String password = properties.value().getProperty("caTrustStorePassword", "ca-truststore-pass"); // ca-truststore-pass

        return KeyStore.getInstance(f, password.toCharArray());
    }

    public SSLContext serverSslContext() throws Exception {
        ServerKey srvKey = this.serverKey();

        KeyManagerFactory kmf;
        KeyStore ks;
        kmf = KeyManagerFactory.getInstance("SunX509");
        ks = srvKey.keyStore();
        kmf.init(ks, srvKey.password());

        TrustManagerFactory tmf;
        KeyStore ts;
        tmf = TrustManagerFactory.getInstance("SunX509");
        ts = this.caTrustStore();
        tmf.init(ts);

        SSLContext ctx;
        ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), SecureRandom.getInstanceStrong());
        return ctx;
    }

    public ClientKey clientKey() {
        return new ClientKey(
                fileNameProperty("clientKeyStore", "client.keystore"),
                this.properties.value().getProperty("clientKeyStorePassword", "client-keystore-pass"),
                this.properties.value().getProperty("clientKeyAlias", "client")
        );
    }


    public Optional<SSLFactory> targetSslFactory() throws Exception {
            ClientKey myKey = this.clientKey();
            return Optional.of(
                    SSLFactory.builder()
                            .withIdentityMaterial(myKey.key(), myKey.password(), myKey.certificateChain())
                            .withProtocols("TLSv1.3")
                            .withTrustMaterial(this.caTrustStore())
                            .withSecureRandom(SecureRandom.getInstanceStrong())
                            .withCiphers("TLS_AES_256_GCM_SHA384")
                            .build());
    }
}
