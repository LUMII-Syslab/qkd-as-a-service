package lv.lumii.pqproxy;

import lv.lumii.keys.ServerKey;
import lv.lumii.keys.ClientKey;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.util.Properties;

public class PQProxyProperties {

    Logger logger = LoggerFactory.getLogger(PQProxyProperties.class);
    private String mainDirectory;
    private Unchecked<Properties> properties;

    public PQProxyProperties(String mainDirectory) {
        this.mainDirectory = mainDirectory;
        this.properties = new Unchecked<>(new Sticky<>(
                () -> loadPropertiesFile(mainDirectory + File.separator + "pqproxy.properties")));
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

    public URI targetUri() throws Exception {
        String host = properties.value().getProperty("targetUri");
        if (host==null)
            throw new Exception("The target URI has not been specified in pqproxy.properties.");
        return new URI(host);
    }

    public int sourcePort() throws Exception {
        int defaultPort = 443;
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

}
