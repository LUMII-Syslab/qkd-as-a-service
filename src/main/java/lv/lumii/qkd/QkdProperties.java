package lv.lumii.qkd;

import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Unchecked;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Properties;

public class QkdProperties {
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
            QkdServer.logger.error("Could not load properties from "+fileName, e);
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
                fileNameProperty("key", "key.keystore"),
                this.properties.value().getProperty("keyPassword", "key-pass"),
                this.properties.value().getProperty("keyAlias", "qkd_user2")
        );
    }

    public KeyStore trustStore() throws Exception {

        String fileName = fileNameProperty("ca", "ca.truststore");
        File f = new File(fileName);

        String password = properties.value().getProperty("caPassword", "ca-truststore-pass"); // ca-truststore-pass

        KeyStore trustStore = KeyStore.getInstance(f, password.toCharArray());
        return trustStore;
    }


}
