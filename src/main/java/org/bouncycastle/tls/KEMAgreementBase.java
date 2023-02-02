package org.bouncycastle.tls;

import org.bouncycastle.tls.crypto.TlsAgreement;
import org.bouncycastle.tls.crypto.TlsSecret;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCrypto;
import org.bouncycastle.tls.crypto.impl.jcajce.JceTlsSecret;
import org.openquantumsafe.Pair;

import java.io.IOException;


/**
 * #pqc-tls #injection
 * A more convenient class to work with KEMs in JCA/JCE.
 * @author Sergejs Kozlovics
 */
public abstract class KEMAgreementBase implements TlsAgreement, KEM {
    protected JcaTlsCrypto crypto;
    protected boolean isServer;
    protected byte[] mySecretKey = null;
    protected byte[] mySecret = null;
    protected byte[] sharedSecret = null;


    public KEMAgreementBase(JcaTlsCrypto crypto, boolean isServer) {
        this.crypto = crypto;
        this.isServer = isServer;
    }


    public boolean isServer() {
        return this.isServer;
    }

    public byte[] publicKey() {
        Pair<byte[],byte[]> p;

        p = this.keyGen(); // factory method call
        byte[] pk = p.getLeft();
        byte[] sk = p.getRight();

        this.mySecretKey = sk;
        return pk;
    }

    public byte[] encapsulatedSecret(byte[] partnerPublicKey) {
        Pair<byte[],byte[]> p;

        p = this.encapsulate(partnerPublicKey); // factory method call
        this.mySecret = p.getLeft();
        return p.getRight();
    }

    public void decapsulateSecret(byte[] ciphertext) {
        this.sharedSecret = this.decapsulate(this.mySecretKey, ciphertext); // factory method call
    }

    public TlsSecret ownSecret() {
        return new JceTlsSecret(this.crypto, this.mySecret); // for non-double KEM
    }

    public TlsSecret sharedSecret() {
        return new JceTlsSecret(this.crypto, this.sharedSecret);
    }

    // excluded TlsAgreement functions:

    public byte[] generateEphemeral() throws IOException {
        throw new IOException("This is a KEM, not a TlsAgreement");
    }

    public void receivePeerValue(byte[] peerEncapsulated) throws IOException {
        throw new IOException("This is a KEM, not a TlsAgreement");
    }

    public TlsSecret calculateSecret() throws IOException {
        throw new IOException("This is a KEM, not a TlsAgreement");
    }

    // included KEM functions (factory methods):

    public abstract Pair<byte[], byte[]> keyGen();
    public abstract Pair<byte[], byte[]> encapsulate(byte[] partnerPublicKey);
    public abstract byte[] decapsulate(byte[] secretKey, byte[] ciphertext);
}
