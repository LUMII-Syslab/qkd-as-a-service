package lv.lumii.qkd;


import lv.lumii.httpws.WsClient;
import org.bouncycastle.asn1.*;
import org.bouncycastle.tls.injection.InjectableAlgorithms;
import org.bouncycastle.tls.injection.InjectableKEMs;
import org.bouncycastle.tls.injection.InjectionPoint;
import org.bouncycastle.tls.injection.kems.KEM;
import org.cactoos.Scalar;
import org.cactoos.scalar.Sticky;
import org.openquantumsafe.Pair;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class InjectableQaasKEM implements KEM {

    private static final int KEY_BITS = 256;
    private static final Nonce nonce = new Nonce();

    private final boolean isServer;
    private final QkdProperties qkdProperties;
    private final InjectionPoint injectionPoint;
    private final InjectableAlgorithms algs; // without QaaS KEM
    private final Scalar<InjectableAlgorithms> algsWithQaasKem;

    public InjectableQaasKEM(boolean isServer, QkdProperties qkdProperties, InjectionPoint injectionPoint, InjectableAlgorithms algs) {
        this.isServer = isServer;
        this.qkdProperties = qkdProperties;
        this.injectionPoint = injectionPoint;
        this.algs = algs;
        this.algsWithQaasKem = new Sticky<>(()->{
            return algs.withKEM("QKD-as-a-Service",
                    0xFEFF, // from our paper; from the reserved-for-private-use range, i.e., 0xFE00..0xFEFF for KEMs
                    () -> InjectableQaasKEM.this,
                    InjectableKEMs.Ordering.BEFORE
            );
        });
    }

    public void inject() {
        try {
            this.injectionPoint.pushAfter(this.algsWithQaasKem.value(), algs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void withdraw() {
        try {
            this.injectionPoint.pop(this.algsWithQaasKem.value());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Pair<byte[], byte[]> keyGen() throws Exception {
        System.out.println("QAAS KEM KEYGEN "+isServer);

        if (this.isServer) {
            //withdraw(); // = lockKEM(0xFEFF); // User2 lock QKD KEM
            return new Pair<>(new byte[]{}, new byte[]{}); // not needed by the server
        } else {
            try {
                withdraw(); // = lockKEM(0xFEFF):

                CompletableFuture<Pair<byte[], byte[]>> result = new CompletableFuture<>();
                long aijaNonce = nonce.nextValue();
                // message formats: https://github.com/LUMII-Syslab/qkd-as-a-service/blob/master/API.md
                WsClient aija = new WsClient(qkdProperties.qaasClientSslFactory(1), qkdProperties.aijaUri(),
                        () -> {
                            // Step 1> reserveKeyAndGetHalf to Aija
                            ASN1EncodableVector v = new ASN1EncodableVector();
                            v.add(new ASN1Integer(1)); // endpoint (function) id
                            v.add(new ASN1Integer(KEY_BITS)); // key length
                            v.add(new ASN1Integer(aijaNonce)); // nonce

                            return new DERSequence(v).getEncoded();
                        },
                        (aijaResponse) -> {
                            // Step 1< parse reserveKeyAndGetHalf response (ASN.1)
                            System.out.println("AIJA RESPONSE:\n" +
                                    byteArrayToString(aijaResponse, " "));
                            ASN1Primitive respObj1 = new ASN1InputStream(aijaResponse).readObject();


                            ASN1Sequence respSeq1 = ASN1Sequence.getInstance(respObj1);
                            if (respSeq1.size() != 7)
                                throw new Exception("Invalid sequence length in reserveKeyAndGetHalf response.");
                            if (ASN1Integer.getInstance(respSeq1.getObjectAt(0)).intValueExact() != 0)
                                throw new Exception("reserveKeyAndGetHalf response returned an error");
                            if (ASN1Integer.getInstance(respSeq1.getObjectAt(1)).intValueExact() != -1)
                                throw new Exception("Invalid reserveKeyAndGetHalf response code");
                            if (ASN1Integer.getInstance(respSeq1.getObjectAt(2)).longValueExact() != aijaNonce+1)
                                throw new Exception("reserveKeyAndGetHalf response returned an invalid nonce");
                            ASN1OctetString keyId = ASN1OctetString.getInstance(respSeq1.getObjectAt(3));
                            ASN1OctetString keyLeft = ASN1OctetString.getInstance(respSeq1.getObjectAt(4));
                            ASN1OctetString hashRight = ASN1OctetString.getInstance(respSeq1.getObjectAt(5));
                            ASN1ObjectIdentifier hashAlgRight = ASN1ObjectIdentifier.getInstance(respSeq1.getObjectAt(6));


                            // Step 3> send getKeyHalf to Brencis
                            long brencisNonce = nonce.nextValue();
                            WsClient brencis = new WsClient(qkdProperties.qaasClientSslFactory(1), qkdProperties.brencisUri(),
                                    () -> {
                                        ASN1EncodableVector v = new ASN1EncodableVector();
                                        v.add(new ASN1Integer(2)); // endpoint (function) id
                                        v.add(new ASN1Integer(KEY_BITS)); // key length
                                        v.add(keyId); // key ID
                                        v.add(new ASN1Integer(brencisNonce));

                                        return new DERSequence(v).getEncoded();
                                    },
                                    (brencisResponse) -> {
                                        // Step 3< parse reserveKeyAndGetHalf response (ASN.1)
                                        ASN1Primitive respObj2 = new ASN1InputStream(brencisResponse).readObject();
                                        ASN1Sequence respSeq2 = ASN1Sequence.getInstance(respObj2);
                                        if (respSeq2.size() != 6)
                                            throw new Exception("Invalid sequence length in getKeyHalf response.");
                                        if (ASN1Integer.getInstance(respSeq2.getObjectAt(0)).intValueExact() != 0)
                                            throw new Exception("getKeyHalf response returned an error");
                                        if (ASN1Integer.getInstance(respSeq2.getObjectAt(1)).intValueExact() != -2)
                                            throw new Exception("Invalid getKeyHalf response code");
                                        if (ASN1Integer.getInstance(respSeq2.getObjectAt(2)).longValueExact() != brencisNonce+1)
                                            throw new Exception("getKeyHalf response returned an invalid nonce");
                                        ASN1OctetString keyRight = ASN1OctetString.getInstance(respSeq2.getObjectAt(3));
                                        ASN1OctetString hashLeft = ASN1OctetString.getInstance(respSeq2.getObjectAt(4));
                                        ASN1ObjectIdentifier hashAlgLeft = ASN1ObjectIdentifier.getInstance(respSeq2.getObjectAt(5));

                                        // comparing the hashes against each other...
                                        assert new Hash(hashAlgLeft, keyLeft).equals(hashLeft);
                                        assert new Hash(hashAlgRight, keyRight).equals(hashRight);

                                        byte[] fullKey = Arrays.copyOf(keyLeft.getOctets(), (int)Math.ceil(KEY_BITS*1.0/8.0));
                                        System.arraycopy(keyRight.getOctets(), 0, fullKey, fullKey.length / 2, fullKey.length / 2);


                                        ASN1EncodableVector pk = new ASN1EncodableVector();
                                        pk.add(keyId);
                                        pk.add(hashLeft);
                                        pk.add(hashRight);

                                        result.complete(new Pair<>(new DERSequence(pk).getEncoded(), fullKey));

                                    },
                                    (ex) -> {
                                        result.completeExceptionally(ex);
                                    },
                                    "Brencis client (in keyGen)"
                            );
                            brencis.connectAndRunAsync();//.connectBlockingAndRunAsync();
                        },
                        (ex) -> {
                            result.completeExceptionally(ex);
                        }, "Aija client (in keyGen)");

                aija.connectBlockingAndRunAsync();

                return result.get();
            } finally {
                inject(); // = unlockKEM(0xFEFF);
            }
        }
    }

    @Override
    public Pair<byte[], byte[]> encapsulate(byte[] partnerPublicKey) throws Exception {
        System.out.println("QAAS KEM ENCAPSULATE "+isServer);
        if (this.isServer) {
            withdraw(); // withdraw QaaS KEM, since the server will communicate with Aija and Brencis using PQC only

            try {
                ASN1Primitive o = new ASN1InputStream(partnerPublicKey).readObject();
                ASN1Sequence seq = ASN1Sequence.getInstance(o);
                if (seq.size() != 3)
                    throw new Exception("Invalid partner's sequence received for Encapsulate.");
                ASN1OctetString keyId = ASN1OctetString.getInstance(seq.getObjectAt(0));
                ASN1OctetString hashLeft = ASN1OctetString.getInstance(seq.getObjectAt(1));
                ASN1OctetString hashRight = ASN1OctetString.getInstance(seq.getObjectAt(2));

                CompletableFuture<Pair<byte[], byte[]>> result = new CompletableFuture<>();
                long aijaNonce = nonce.nextValue();
                // message formats: https://github.com/LUMII-Syslab/qkd-as-a-service/blob/master/API.md
                WsClient aija = new WsClient(qkdProperties.qaasClientSslFactory(2), qkdProperties.aijaUri(),
                        () -> {
                            // Step 4> getKeyHalf to Aija
                            ASN1EncodableVector v = new ASN1EncodableVector();
                            v.add(new ASN1Integer(2)); // endpoint (function) id
                            v.add(new ASN1Integer(KEY_BITS)); // key length
                            v.add(keyId); // key ID
                            v.add(new ASN1Integer(aijaNonce));

                            return new DERSequence(v).getEncoded();
                        },
                        (aijaResponse) -> {
                            // Step 4< parse getKeyHalf response (ASN.1)
                            ASN1Primitive respObj1 = new ASN1InputStream(aijaResponse).readObject();
                            ASN1Sequence respSeq1 = ASN1Sequence.getInstance(respObj1);
                            if (respSeq1.size() != 6)
                                throw new Exception("Invalid sequence length in getKeyHalf response.");
                            if (ASN1Integer.getInstance(respSeq1.getObjectAt(0)).intValueExact() != 0)
                                throw new Exception("getKeyHalf response returned an error");
                            if (ASN1Integer.getInstance(respSeq1.getObjectAt(1)).intValueExact() != -2)
                                throw new Exception("Invalid getKeyHalf response code");
                            if (ASN1Integer.getInstance(respSeq1.getObjectAt(2)).longValueExact() != aijaNonce + 1)
                                throw new Exception("getKeyHalf response returned an invalid nonce");
                            ASN1OctetString keyLeft = ASN1OctetString.getInstance(respSeq1.getObjectAt(3));
                            ASN1OctetString hashRight1 = ASN1OctetString.getInstance(respSeq1.getObjectAt(4));
                            ASN1ObjectIdentifier hashAlgRight1 = ASN1ObjectIdentifier.getInstance(respSeq1.getObjectAt(5));

                            // comparing expected hashRight and returned hashRight1:
                            assert new Hash(hashRight).equals(hashRight1);


                            // Step 5> send getKeyHalf to Brencis
                            long brencisNonce = nonce.nextValue();
                            WsClient brencis = new WsClient(qkdProperties.qaasClientSslFactory(2), qkdProperties.brencisUri(),
                                    () -> {
                                        ASN1EncodableVector v = new ASN1EncodableVector();
                                        v.add(new ASN1Integer(2)); // endpoint (function) id
                                        v.add(new ASN1Integer(KEY_BITS)); // key length
                                        v.add(keyId); // key ID
                                        v.add(new ASN1Integer(brencisNonce));

                                        return new DERSequence(v).getEncoded();
                                    },
                                    (brencisResponse) -> {
                                        // Step 5< parse getKeyHalf response (ASN.1)

                                        ASN1Primitive respObj2 = new ASN1InputStream(brencisResponse).readObject();
                                        ASN1Sequence respSeq2 = ASN1Sequence.getInstance(respObj2);
                                        if (respSeq2.size() != 6)
                                            throw new Exception("Invalid sequence length in getKeyHalf response.");
                                        if (ASN1Integer.getInstance(respSeq2.getObjectAt(0)).intValueExact() != 0)
                                            throw new Exception("getKeyHalf response returned an error");
                                        if (ASN1Integer.getInstance(respSeq2.getObjectAt(1)).intValueExact() != -2)
                                            throw new Exception("Invalid getKeyHalf response code");
                                        if (ASN1Integer.getInstance(respSeq2.getObjectAt(2)).longValueExact() != brencisNonce + 1)
                                            throw new Exception("getKeyHalf response returned an invalid nonce");
                                        ASN1OctetString keyRight = ASN1OctetString.getInstance(respSeq2.getObjectAt(3));
                                        ASN1OctetString hashLeft2 = ASN1OctetString.getInstance(respSeq2.getObjectAt(4));
                                        ASN1ObjectIdentifier hashAlgLeft2 = ASN1ObjectIdentifier.getInstance(respSeq2.getObjectAt(5));

                                        // comparing expected hashLeft and returned hashLeft1:
                                        assert new Hash(hashLeft).equals(hashLeft2);

                                        // comparing the hashes against each other...
                                        assert new Hash(hashAlgLeft2, keyLeft).equals(hashLeft);
                                        assert new Hash(hashAlgRight1, keyRight).equals(hashRight);

                                        byte[] fullKey = Arrays.copyOf(keyLeft.getOctets(), (int)Math.ceil(KEY_BITS*1.0/8.0));
                                        System.arraycopy(keyRight.getOctets(), 0, fullKey, fullKey.length / 2, fullKey.length / 2);

                                        byte[] fullHash = new Hash(hashAlgLeft2, fullKey).value(); // just choose the same hash algorithm used for the left half

                                        ASN1EncodableVector ct = new ASN1EncodableVector();
                                        ct.add(new DEROctetString(fullHash));
                                        ct.add(hashAlgLeft2);
                                        byte[] ctEncoded = new DERSequence(ct).getEncoded();

                                        System.out.println("QaaS SHARED SECRET at server: " + byteArrayToString(fullKey));

                                        result.complete(new Pair<>(fullKey, ctEncoded));

                                    },
                                    (ex) -> {
                                        result.completeExceptionally(ex);
                                    },
                                    "Brencis client (in encapsulate)"
                            );
                            brencis.connectBlockingAndRunAsync();
                        },
                        (ex) -> {
                            result.completeExceptionally(ex);
                        },
                        "Aija client (in encapsulate)");
                aija.connectBlockingAndRunAsync();

                return result.get();
            }
            finally {
                inject(); // re-inject QaaS back
            }
        } else { // client
            return new Pair<>(new byte[]{}, new byte[]{});
        }
    }

    @Override
    public byte[] decapsulate(byte[] secretKey, byte[] ciphertext) throws Exception {
        System.out.println("QAAS KEM DECAPSULATE "+isServer);

        if (this.isServer) {
            // inject(); // = unlockKEM(0xFEFF); // User2 unlock QKD KEM
            return new byte[]{};
        } else {
            ASN1Primitive o = new ASN1InputStream(ciphertext).readObject();
            ASN1Sequence seq = ASN1Sequence.getInstance(o);
            if (seq.size() != 2)
                throw new Exception("Invalid partner's sequence received for Decapsulate.");
            ASN1OctetString fullHash = ASN1OctetString.getInstance(seq.getObjectAt(0));
            ASN1ObjectIdentifier hashAlgOid = ASN1ObjectIdentifier.getInstance(seq.getObjectAt(1));

            // comparing the received and expected hash
            assert new Hash(hashAlgOid, secretKey).equals(fullHash);

            System.out.println("QaaS SHARED SECRETat client: " + byteArrayToString(secretKey));

            return secretKey;
        }

    }

    public static String byteArrayToString(byte[] a) {
        return byteArrayToString(a, "");
    }

    public static String byteArrayToString(byte[] a, String delim) {
        String s = "";
        for (byte b : a) {
            if (s.length() > 0)
                s += delim;
            s += String.format("%02x", b);
        }
        return s;
    }

    public static byte[] hexStringToByteArray(String s) {
        s = s.replaceAll(" ", ""); // remove all spaces
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
