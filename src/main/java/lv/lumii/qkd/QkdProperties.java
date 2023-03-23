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

    public URI user2Uri() throws Exception {
        String uri = properties.value().getProperty("serverUri", "wss://localhost:443");
        return new URI(uri);
    }

    public URI aijaUri() throws Exception {
        String uri = properties.value().getProperty("aijaUri", "wss://localhost:4001");
        return new URI(uri);
    }

    public URI brencisUri() throws Exception {
        String uri = properties.value().getProperty("brencisUri", "wss://localhost:4002");
        return new URI(uri);
    }

    public KeyStore caTrustStore() throws Exception {

        String fileName = fileNameProperty("caTrustStore", "ca.truststore");
        File f = new File(fileName);

        String password = properties.value().getProperty("caTrustStorePassword", "ca-truststore-pass"); // ca-truststore-pass

        return KeyStore.getInstance(f, password.toCharArray());
    }


    /**
     * Returns the double-purpose User1 key.
     * @return the User1 key used
     * as a client key representing User1 to User2
     * and as a client key for connecting to Aija and Brencis
     */
    public ClientKey user1Key() {
        return new ClientKey(
                fileNameProperty("clientKeyStore", "client.keystore"),
                this.properties.value().getProperty("clientKeyStorePassword", "client-keystore-pass"),
                this.properties.value().getProperty("clientKeyAlias", "client")
        );
    }


    /**
     * Returns the double-purpose User2 key.
     * @return the User2 key used
     * as a server key representing User2 to User1
     * and as a client key for connecting to Aija and Brencis
     */
    public ServerKey user2Key() {
        return new ServerKey(
                fileNameProperty("serverKeyStore", "server.keystore"),
                this.properties.value().getProperty("serverKeyStorePassword", "server-keystore-pass"),
                this.properties.value().getProperty("serverKeyAlias", "server")
                //"qkd_user2")
        );
    }

    public Optional<SSLFactory> user1SslFactory() throws Exception {
            ClientKey myKey = this.user1Key();
            return Optional.of(
                    SSLFactory.builder()
                            .withIdentityMaterial(myKey.key(), myKey.password(), myKey.certificateChain())
                            .withProtocols("TLSv1.3")
                            .withTrustMaterial(this.caTrustStore())
                            .withSecureRandom(SecureRandom.getInstanceStrong())
                            .withCiphers("TLS_AES_256_GCM_SHA384")
                            .build());
    }


    public SSLContext user2SslContext() throws Exception {
        ServerKey srvKey = this.user2Key();

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

    public Optional<SSLFactory> qaasClientSslFactory(int userNo) throws Exception {
        ClientKey user1key = this.user1Key();
        ServerKey user2key = this.user2Key();
        return Optional.of(
                SSLFactory.builder()
                        .withIdentityMaterial(
                                userNo==1?user1key.key():user2key.key(),
                                userNo==1? user1key.password() : user2key.password(),
                                userNo==1? user1key.certificateChain() : user2key.certificateChain())
                        .withProtocols("TLSv1.3")
                        .withTrustMaterial(this.caTrustStore())
                        .withSecureRandom(SecureRandom.getInstanceStrong())
                        .withCiphers("TLS_AES_256_GCM_SHA384")
                        .build());
    }

}
