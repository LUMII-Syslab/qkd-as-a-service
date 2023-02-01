package lv.lumii.qkd;

import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Unchecked;

import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;

public class QkdServerKey {

    private char[] password;
    private String alias;
    private Unchecked<KeyStore> keyStore;

    public QkdServerKey(String fileName, String password, String alias) {
        this.password = password.toCharArray();
        this.alias = alias;
        this.keyStore = new Unchecked<>(new Sticky<>(()->loadKeyStore(fileName)));
    }

    private KeyStore loadKeyStore(String fileName) throws Exception {
        KeyStore clientKeyStore  = KeyStore.getInstance("PKCS12");
        // ^^^ If "Algorithm HmacPBESHA256 not available" error => need jdk16+ (new pkx format hash)

        //variant:        KeyStore clientKeyStore = KeyStore.getInstance(f, password.toCharArray());

        File f = new File(fileName);
        FileInputStream instream = new FileInputStream(f);
        try {
            clientKeyStore.load(instream, password);
        }
        finally {
            instream.close();
        }
        return clientKeyStore;
    }

    public Key key() throws Exception {
        return this.keyStore.value().getKey(alias, password);
    }

    public char[] password() {
        return this.password;
    }

    public KeyStore keyStore() {
        return this.keyStore.value();
    }

    /*public Certificate[] certificateChain() throws Exception {
        Certificate[] arr = this.keyStore.value().getCertificateChain(this.alias);
        return arr;
    }*/
}
