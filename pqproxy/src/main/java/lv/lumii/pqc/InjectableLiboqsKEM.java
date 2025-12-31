package lv.lumii.pqc;

import org.bouncycastle.tls.injection.kems.KEM;
import org.openquantumsafe.Pair;

import org.openquantumsafe.KeyEncapsulation;

import java.security.SecureRandom;

public class InjectableLiboqsKEM implements KEM {

    private String liboqsKemName; // e.g., "FrodoKEM-640-AES";
    private int codePoint; // e.g., 0x0200 for oqs_frodo640aes_codepoint
    private KeyEncapsulation kem;

    public InjectableLiboqsKEM(String liboqsKemName, int codePoint) {
        this.kem = new org.openquantumsafe.KeyEncapsulation(liboqsKemName);
        this.codePoint = codePoint;
    }

    public int codePoint() {
        return this.codePoint;
    }

    @Override
    public Pair<byte[], byte[]> keyGen() {
        // at the client side:

        // if via liboqs JNI + DLL:
        byte[] myPublicKey = kem.generate_keypair().clone();
        byte[] myPrivateKey = kem.export_secret_key().clone();

        return new Pair<>(myPublicKey, myPrivateKey);
    }

    @Override
    public Pair<byte[], byte[]> encapsulate(byte[] partnerPublicKey) {
        // at the server side:
        // if via liboqs JNI + DLL:
        Pair<byte[], byte[]> pair = kem.encap_secret(partnerPublicKey);
        byte[] ciphertext = pair.getLeft();
        byte[] semiSecret = pair.getRight();
        return new Pair<>(semiSecret, ciphertext);
    }

    @Override
    public byte[] decapsulate(byte[] secretKey, byte[] ciphertext) {
        // at the client side

        byte[] sharedSecret;

        // assert: this.secretKey == secretKey
        // if via libqs JNI + DLL:
        sharedSecret = kem.decap_secret(ciphertext);

        // if via liboqs JNI + DLL:
        this.kem.dispose_KEM();

        return sharedSecret;
    }
}
