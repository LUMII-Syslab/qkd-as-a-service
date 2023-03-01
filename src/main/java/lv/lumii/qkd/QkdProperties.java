package lv.lumii.qkd;

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

    public URI remoteUri() throws Exception {
        String host = properties.value().getProperty("host", "localhost");
        host = "https://"+host+":"+port();
        return new URI(host);
    }

    public int port() throws Exception {
        int defaultPort = 443;
        String s = properties.value().getProperty("port", defaultPort+"");
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

    public QkdServerKey serverKey() {
        return new QkdServerKey(
                fileNameProperty("serverKeyStore", "server.keystore"),
                this.properties.value().getProperty("serverKeyStorePassword", "server-keystore-pass"),
                this.properties.value().getProperty("serverKeyAlias", "server")
                        //"qkd_user2")
        );
    }

    public KeyStore trustStore() throws Exception {

        String fileName = fileNameProperty("caTrustStore", "ca.truststore");
        File f = new File(fileName);

        String password = properties.value().getProperty("caTrustStorePassword", "ca-truststore-pass"); // ca-truststore-pass

        return KeyStore.getInstance(f, password.toCharArray());
    }


}
