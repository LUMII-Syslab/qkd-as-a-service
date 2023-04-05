package lv.lumii.qkd;

import lv.lumii.httpws.WsClient;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.digests.NullDigest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.pqc.crypto.frodo.FrodoKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.frodo.FrodoKeyPairGenerator;
import org.bouncycastle.pqc.crypto.frodo.FrodoParameters;
import org.bouncycastle.pqc.crypto.sphincsplus.SPHINCSPlusParameters;
import org.bouncycastle.pqc.crypto.sphincsplus.SPHINCSPlusPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.sphincsplus.SPHINCSPlusPublicKeyParameters;
import org.bouncycastle.pqc.crypto.sphincsplus.SPHINCSPlusSigner;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.provider.sphincsplus.BCSPHINCSPlusPrivateKey;
import org.bouncycastle.pqc.jcajce.provider.sphincsplus.BCSPHINCSPlusPublicKey;
import org.bouncycastle.pqc.jcajce.provider.sphincsplus.SPHINCSPlusKeyFactorySpi;
import org.bouncycastle.pqc.jcajce.provider.sphincsplus.SignatureSpi;
import org.bouncycastle.tls.SignatureAndHashAlgorithm;
import org.bouncycastle.tls.crypto.TlsSigner;
import org.bouncycastle.tls.crypto.TlsStreamSigner;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCrypto;
import org.bouncycastle.tls.injection.kems.InjectedKEMs;
import org.bouncycastle.tls.injection.kems.KEMAgreementBase;
import org.bouncycastle.tls.injection.keys.BC_ASN1_Converter;
import org.bouncycastle.tls.injection.sigalgs.InjectedSigAlgorithms;
import org.bouncycastle.tls.injection.sigalgs.InjectedSigVerifiers;
import org.bouncycastle.tls.injection.sigalgs.InjectedSigners;
import org.bouncycastle.util.Pack;
import org.openquantumsafe.KeyEncapsulation;
import org.openquantumsafe.Pair;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.*;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The class for injecting PQC algorithms used for our experiments (~post-quantum agility)
 * <p>
 * #pqc-tls #injection
 *
 * @author Sergejs Kozlovics
 */
public class InjectableQKD {

    // Signature Scheme code points and OIDs
    /*
     * 1.3.9999.6.7.1 SPHINCS+ OID from open-quantum-safe;
     * ALL oqs SIG code points: https://github.com/open-quantum-safe/openssl/blob/OQS-OpenSSL_1_1_1-stable/oqs-template/oqs-sig-info.md
     */
    //public static final ASN1ObjectIdentifier oqs_sphincsshake256128frobust_oid = new ASN1ObjectIdentifier("1.3.9999.6.7").branch("1");
    //public static final ASN1ObjectIdentifier oqs_sphincssha256256frobust_oid = new ASN1ObjectIdentifier("1.3.9999.6.6").branch("1");
    public static final ASN1ObjectIdentifier oqs_sphincssha256128frobust_oid = new ASN1ObjectIdentifier("1.3.9999.6.4").branch("1");
    /*
     * RFC 8446 reserved for private use (0xFE00..0xFFFF)
     */
    //public static final int oqs_sphincsshake256128frobust_signaturescheme_codepoint = 0xfe7a;
    // ^^^ when compiling OQS openssl 1.1.1, go to openssl/oqs-template/generate.yml and enable this algorithm!
    //     then invoke: python3 oqs-template/generate.py
    //public static final int oqs_sphincssha256256frobust_signaturescheme_codepoint = 0xfe72;
    public static final int oqs_sphincssha256128frobust_signaturescheme_codepoint = 0xfe5e;
    // KEM code points
    /*
    ??? RFC 4492 reserved ecdhe_private_use (0xFE00..0xFEFF)

    ALL oqs KEM code points: https://github.com/open-quantum-safe/openssl/blob/OQS-OpenSSL_1_1_1-stable/oqs-template/oqs-kem-info.md
     */
    private static final int oqs_frodo640shake_codepoint = 0x0201;
    public static ExecutorService qaasExecutor = Executors.newSingleThreadExecutor();
    public static Nonce nonce = new Nonce();
    private static final String OQS_SIG_NAME =
            //"SPHINCS+-SHAKE256-128f-robust"
            "SPHINCS+-SHA256-128f-robust";
    //private static SPHINCSPlusParameters sphincsPlusParameters = SPHINCSPlusParameters.shake256_128f;
    //private static SPHINCSPlusParameters sphincsPlusParameters = SPHINCSPlusParameters.shake_128f;
    private static final SPHINCSPlusParameters sphincsPlusParameters = SPHINCSPlusParameters.sha2_128f;
    private static final int sphincsPlusParametersAsInt = SPHINCSPlusParameters.getID(sphincsPlusParameters);

    public static void inject(InjectedKEMs.InjectionOrder injectionOrder, QkdProperties qkdProperties) {
        // PQC signatures are huge; increasing the max handshake size:
        System.setProperty("jdk.tls.maxHandshakeMessageSize", String.valueOf(32768 * 32));
        //System.setProperty("jdk.tls.client.SignatureSchemes", "SPHINCS+"); // comma-separated


        //ASN1ObjectIdentifier sigOid = InjectablePQC.oqs_sphincsshake256128frobust_oid;
        ASN1ObjectIdentifier sigOid = InjectableQKD.oqs_sphincssha256128frobust_oid;
        int sigCodePoint = InjectableQKD.oqs_sphincssha256128frobust_signaturescheme_codepoint;
        int sphincsPlusPKLength = 32;
        int sphincsPlusSKLength = 64;
        // ^^^ see: https://github.com/sphincs/sphincsplus

        InjectedKEMs.injectionOrder = injectionOrder;

        InjectedSigAlgorithms.injectSigAndHashAlgorithm(
                "SPHINCS+",//"SPHINCSPLUS",
                sigOid,
                sigCodePoint,
                new BC_ASN1_Converter() {
                    @Override
                    public boolean isSupportedParameter(AsymmetricKeyParameter someKey) {
                        return someKey instanceof SPHINCSPlusPublicKeyParameters ||
                                someKey instanceof SPHINCSPlusPrivateKeyParameters;
                    }

                    @Override
                    public AsymmetricKeyParameter createPrivateKeyParameter(PrivateKeyInfo keyInfo) throws IOException {
                        byte[] keyEnc = ASN1OctetString.getInstance(keyInfo.parsePrivateKey()).getOctets(); // £££
                        // ^^^ if it were: keyInfo.getEncoded() contains also additional stuff, including OID
                        SPHINCSPlusParameters spParams = sphincsPlusParameters;
                        return new SPHINCSPlusPrivateKeyParameters(spParams, Arrays.copyOfRange(keyEnc, 0, sphincsPlusSKLength));
                    }

                    @Override
                    public PrivateKeyInfo createPrivateKeyInfo(AsymmetricKeyParameter privateKey, ASN1Set attributes) throws IOException {
                        SPHINCSPlusPrivateKeyParameters params = (SPHINCSPlusPrivateKeyParameters) privateKey;

                        byte[] encoding = params.getEncoded(); // ££££
                        byte[] pubEncoding = params.getEncodedPublicKey();

                        // remove alg params (4 bytes)
                        encoding = Arrays.copyOfRange(encoding, 4, encoding.length);
                        pubEncoding = Arrays.copyOfRange(pubEncoding, 4, pubEncoding.length);

                        AlgorithmIdentifier algorithmIdentifier =
                                new AlgorithmIdentifier(sigOid);
                        //new AlgorithmIdentifier(Utils.sphincsPlusOidLookup(params.getParameters()));  // by SK: here BC gets its algID!!!  @@@ @@@
                        return new PrivateKeyInfo(algorithmIdentifier, new DEROctetString(encoding), attributes, pubEncoding);
                    }

                    @Override
                    public AsymmetricKeyParameter createPublicKeyParameter(SubjectPublicKeyInfo keyInfo, Object defaultParams) throws IOException {
                        byte[] wrapped = keyInfo.getEncoded(); // ASN1 wrapped
                        byte[] keyEnc = Arrays.copyOfRange(wrapped, wrapped.length - sphincsPlusPKLength, wrapped.length); // ASN1OctetString.getInstance(keyInfo.parsePublicKey()).getOctets();
                        AlgorithmIdentifier alg = keyInfo.getAlgorithm();
                        ASN1ObjectIdentifier oid = alg.getAlgorithm();
                        int i = sphincsPlusParametersAsInt; // TODO: get i from associated oid
                        SPHINCSPlusParameters spParams = SPHINCSPlusParameters.getParams(i);
                        return new SPHINCSPlusPublicKeyParameters(spParams, keyEnc);
                    }

                    @Override
                    public SubjectPublicKeyInfo createSubjectPublicKeyInfo(AsymmetricKeyParameter publicKey) throws IOException {
                        SPHINCSPlusPublicKeyParameters params = (SPHINCSPlusPublicKeyParameters) publicKey;

                        byte[] encoding = params.getEncoded();

                        // remove the first 4 bytes (alg. params)
                        encoding = Arrays.copyOfRange(encoding, 4, encoding.length);

                        AlgorithmIdentifier algorithmIdentifier = new AlgorithmIdentifier(sigOid);//??? -- does not matter
                        // new AlgorithmIdentifier(Utils.sphincsPlusOidLookup(params.getParameters())); // by SK: here BC gets its algID!!!
                        return new SubjectPublicKeyInfo(algorithmIdentifier, new DEROctetString(encoding));
                    }
                },
                new SPHINCSPlusKeyFactorySpi(),
                (PublicKey pk) -> {
                    if (pk instanceof BCSPHINCSPlusPublicKey)
                        return new SphincsPlusSignatureSpi();
                    else
                        throw new RuntimeException("Only SPHINCS+ is supported in this implementation of InjectedSignatureSpi.Factory");
                }
        );
        InjectedSigners.injectSigner("SPHINCS+", (JcaTlsCrypto crypto, PrivateKey privateKey) -> {
            assert (privateKey instanceof BCSPHINCSPlusPrivateKey);

            BCSPHINCSPlusPrivateKey sk = (BCSPHINCSPlusPrivateKey) privateKey;
            InjectableSphincsPlusTlsSigner signer = new InjectableSphincsPlusTlsSigner();

            SPHINCSPlusPrivateKeyParameters p = (SPHINCSPlusPrivateKeyParameters) sk.getKeyParams();

            byte[] keys = p.getEncoded(); // TODO: read sphincsPlusParameters from the first 4 big-endian bytes
            SPHINCSPlusPrivateKeyParameters newP = new SPHINCSPlusPrivateKeyParameters(sphincsPlusParameters,
                    Arrays.copyOfRange(keys, 4, keys.length));
            p = newP;
            signer.init(true, p);

            return signer;
        });
        InjectedSigVerifiers.injectVerifier(
                sigCodePoint,
                (InjectedSigVerifiers.VerifySignatureFunction) (data, key, signature) -> {
                    int from = 26; // see der.md
                    int priorTo = key.length;
                    //SPHINCSPlusSigner signer = new SPHINCSPlusSigner(); -- otherwise we need to modify SignatureSpi
                    InjectableSphincsPlusTlsSigner signer = new InjectableSphincsPlusTlsSigner();

                    byte[] pubKey = Arrays.copyOfRange(key, from, priorTo);
                    SPHINCSPlusPublicKeyParameters params = new SPHINCSPlusPublicKeyParameters(
                            sphincsPlusParameters, pubKey);
                    signer.init(false, params);
                    boolean b = signer.verifySignature(data, signature.getSignature());
                    return b;
                });

        // offer QKD as the first KEM
        InjectedKEMs.injectKEM(
                0xFEFF, // from our paper; from the reserved-for-private-use range, i.e., 0xFE00..0xFEFF for KEMs
                "QKD-as-a-Service",
                (crypto, kemCodePoint, isServer) -> new InjectableQaaSKEM(crypto, isServer, qkdProperties));

        InjectedKEMs.injectKEM(oqs_frodo640shake_codepoint, "FrodoKEM-640-SHAKE",
                (crypto, kemCodePoint, isServer) -> new InjectableFrodoKEMAgreement(crypto, "FrodoKEM-640-SHAKE", isServer));


        BouncyCastleJsseProvider jsseProvider = new BouncyCastleJsseProvider();
        Security.insertProviderAt(jsseProvider, 1);

        BouncyCastlePQCProvider bcProvider = new BouncyCastlePQCProvider(); // BCPQC
        Security.insertProviderAt(bcProvider, 1);
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
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static void main(String[] args) {
        // if via liboqs JNI + DLL:

        KeyEncapsulation kem1 = new KeyEncapsulation("FrodoKEM-640-SHAKE");
        KeyEncapsulation kem2 = new KeyEncapsulation("FrodoKEM-640-SHAKE");

        byte[] pk1 = kem1.generate_keypair();
        byte[] sk1 = kem1.export_secret_key();

        byte[] pk2 = kem2.generate_keypair();
        byte[] sk2 = kem2.export_secret_key();

        // pk1 =>
        // <= pk2

        Pair<byte[], byte[]> pair1 = kem1.encap_secret(pk2);
        byte[] my1 = pair1.getRight();
        byte[] enc1 = pair1.getLeft();

        Pair<byte[], byte[]> pair2 = kem2.encap_secret(pk1);
        byte[] my2 = pair2.getRight();
        byte[] enc2 = pair2.getLeft();

        byte[] d1 = kem1.decap_secret(enc2);
        byte[] d2 = kem2.decap_secret(enc1);

        System.out.println(byteArrayToString(d1));
        System.out.println(byteArrayToString(my1));
        System.out.println(byteArrayToString(d2));
        System.out.println(byteArrayToString(my2));

        /*
        for (String s : org.openquantumsafe.Sigs.get_enabled_sigs()) {
            //System.out.println("SIG "+s);
        }
        String pkStr = "8776619e7fc2ca19b0be40157190208680007c01b855256123e2866ae71ad34616af34d2a08542a6fcd8b9ceab9ea4fa4bf640a5cd866f87aad16a971603e173";
        byte[] sk = hexStringToByteArray(pkStr);
        byte[] pk = Arrays.copyOfRange(sk, sk.length-32, sk.length);
        byte[] message = new byte[] {};// {0, 1, 2};

        System.out.printf("Signing message '%s'...\n", byteArrayToString(message));

        byte[] oqsSignature = InjectableSphincsPlusTlsSigner.generateSignature_oqs(message, sk);
        byte[] bcSignature = InjectableSphincsPlusTlsSigner.generateSignature_bc(message, sk);
        System.out.printf("SECRET KEY:\n%s\n", InjectablePQC.byteArrayToString(sk));

        //System.out.printf("OQS SIGNATURE:\n%s\n", InjectablePQC.byteArrayToString(oqsSignature));
        System.out.printf("OQS SIGNATURE VERIFY: oqs:%b bc:%b\n",
                InjectableSphincsPlusTlsSigner.verifySignature_oqs(message, oqsSignature, pk),
                InjectableSphincsPlusTlsSigner.verifySignature_bc(message, oqsSignature, pk));
        //System.out.printf("BC SIGNATURE:\n%s\n", InjectablePQC.byteArrayToString(bcSignature));
        System.out.printf("BC SIGNATURE VERIFY: oqs:%b bc:%b\n",
                InjectableSphincsPlusTlsSigner.verifySignature_oqs(message, bcSignature, pk),
                InjectableSphincsPlusTlsSigner.verifySignature_bc(message, bcSignature, pk));
*/
    }

    ///// TESTS /////

    public static String getTlsProvider() {
        Provider tlsProvider = null;
        try {
            tlsProvider = SSLContext.getInstance("TLS").getProvider();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return tlsProvider.getName();
    }

    public static class SphincsPlusSignatureSpi extends SignatureSpi { // non-private, otherwise, Java reflection doesn't see it
        public SphincsPlusSignatureSpi() {
            super(new NullDigest(), new InjectableSphincsPlusTlsSigner());
        }
    }

    public static class InjectableSphincsPlusTlsSigner extends SPHINCSPlusSigner implements TlsSigner {

        //BCSPHINCSPlusPrivateKey privateKey = null;
        //public InjectableSphincsPlusTlsSigner() {
        //  super();
        //}

        public SPHINCSPlusPublicKeyParameters pkParams = null;
        //public InjectableSphincsPlusTlsSigner(BCSPHINCSPlusPrivateKey privateKey) {
        //  super();
        //this.privateKey = privateKey;
        //}
        private SPHINCSPlusPrivateKeyParameters skParams = null;

        public static byte[] generateSignature_oqs(byte[] message, byte[] sk) {
            org.openquantumsafe.Signature oqsSigner = new org.openquantumsafe.Signature(
                    OQS_SIG_NAME,
                    sk);

            byte[] oqsSignature = oqsSigner.sign(message);
            return oqsSignature;
        }

        public static byte[] generateSignature_bc(byte[] message, byte[] sk) {
            SPHINCSPlusSigner signer = new SPHINCSPlusSigner();
            signer.init(true, new SPHINCSPlusPrivateKeyParameters(sphincsPlusParameters, sk));
            //signer.initForSigning(new SPHINCSPlusPrivateKeyParameters(SPHINCSPlusParameters.shake_128f, sk));
            byte[] bcSignature = signer.generateSignature(message);
            return bcSignature;
        }

        public static boolean verifySignature_oqs(byte[] message, byte[] signature, byte[] publicKey) {
            org.openquantumsafe.Signature oqsVerifier = new org.openquantumsafe.Signature(
                    OQS_SIG_NAME);
            boolean result = oqsVerifier.verify(message, signature, publicKey);
            return result;
        }

        public static boolean verifySignature_bc(byte[] message, byte[] signature, byte[] publicKey) {
            SPHINCSPlusSigner verifier = new SPHINCSPlusSigner();
            verifier.init(false, new SPHINCSPlusPublicKeyParameters(sphincsPlusParameters, publicKey));
            boolean result = verifier.verifySignature(message, signature);
            return result;
        }

        @Override
        public void init(boolean forSigning, CipherParameters param) {
            super.init(forSigning, param);
            if (param instanceof SPHINCSPlusPrivateKeyParameters) {
                skParams = (SPHINCSPlusPrivateKeyParameters) param;
                pkParams = new SPHINCSPlusPublicKeyParameters(skParams.getParameters(), skParams.getPublicKey()); // needed for verifiers
            } else
                pkParams = (SPHINCSPlusPublicKeyParameters) param;
        }

        @Override
        public byte[] generateRawSignature(SignatureAndHashAlgorithm algorithm, byte[] hash) throws IOException {
            return this.generateSignature(hash);
        }

        @Override
        public TlsStreamSigner getStreamSigner(SignatureAndHashAlgorithm algorithm) throws IOException {
            return new MyStreamSigner(algorithm);
        }

        @Override
        public byte[] generateSignature(byte[] message) {
            // override with oqs implementation
            byte[] sk = skParams.getEncoded();
            int sphincsPlusParams = Pack.bigEndianToInt(sk, 0);
            sk = Arrays.copyOfRange(sk, 4, sk.length);


            byte[] pk = skParams.getPublicKey();
            byte[] oqsSignature = InjectableSphincsPlusTlsSigner.generateSignature_oqs(message, sk);
            byte[] bcSignature = InjectableSphincsPlusTlsSigner.generateSignature_bc(message, sk);
            System.out.printf("SECRET KEY:\n%s\n", InjectableQKD.byteArrayToString(sk));

            //System.out.printf("OQS SIGNATURE:\n%s\n", InjectablePQC.byteArrayToString(oqsSignature));
            System.out.printf("OQS SIGNATURE VERIFY: oqs:%b bc:%b\n",
                    InjectableSphincsPlusTlsSigner.verifySignature_oqs(message, oqsSignature, pk),
                    InjectableSphincsPlusTlsSigner.verifySignature_bc(message, oqsSignature, pk));
            //System.out.printf("BC SIGNATURE:\n%s\n", InjectablePQC.byteArrayToString(bcSignature));
            System.out.printf("BC SIGNATURE VERIFY: oqs:%b bc:%b\n",
                    InjectableSphincsPlusTlsSigner.verifySignature_oqs(message, bcSignature, pk),
                    InjectableSphincsPlusTlsSigner.verifySignature_bc(message, bcSignature, pk));

            return oqsSignature;
        }


        @Override
        public boolean verifySignature(byte[] message, byte[] signature) {

            // override with oqs implementation
            byte[] pk = pkParams.getEncoded();
            int sphincsPlusParams = Pack.bigEndianToInt(pk, 0);
            // 4 bytes big endian - params ID
            pk = Arrays.copyOfRange(pk, 4, pk.length);

            boolean result = InjectableSphincsPlusTlsSigner.verifySignature_oqs(message, signature, pk);
            return result;
        }

        private class MyStreamSigner implements TlsStreamSigner {

            SignatureAndHashAlgorithm algorithm;
            private final ByteArrayOutputStream os = new ByteArrayOutputStream();

            public MyStreamSigner(SignatureAndHashAlgorithm algorithm) {
                this.algorithm = algorithm;
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return os;
            }

            @Override
            public byte[] getSignature() throws IOException {
                //return InjectableSphincsPlusTlsSigner.this.generateRawSignature(algorithm, os.toByteArray());

                byte[] data = os.toByteArray();//Arrays.copyOfRange(os.toByteArray(), 0, os.size());
                byte[] sk = skParams.getEncoded();

                byte[] signature = InjectableSphincsPlusTlsSigner.this.generateSignature(data);
                return signature;
            }
        }
    }

    public static class InjectableFrodoKEMAgreement extends KEMAgreementBase {
        FrodoKeyPairGenerator kemGen; // - if via BC
        private final KeyEncapsulation kem; //- if via liboqs + JNI + DLL

        public InjectableFrodoKEMAgreement(JcaTlsCrypto crypto, String kemName, boolean isServer) {
            super(crypto, isServer);
            this.kem = new KeyEncapsulation(kemName); //- if via liboqs + JNI + DLL

            this.kemGen = new FrodoKeyPairGenerator();
            this.kemGen.init(new FrodoKeyGenerationParameters(new SecureRandom(), FrodoParameters.frodokem640shake));
        }


        // if pure Java (BouncyCastle):
/*            FrodoPrivateKeyParameters priv = new FrodoPrivateKeyParameters(FrodoParameters.frodokem640shake, this.clientPrivateKey);
            FrodoKEMExtractor ext = new FrodoKEMExtractor(priv);

            byte[] otherSecret = ext.extractSecret(this.serverEnsapsulated);


            // bitwise XOR of mySecred and otherSecret
            BitSet bsa = BitSet.valueOf(mySecret);
            BitSet bsb = BitSet.valueOf(otherSecret);

            bsa.xor(bsb);
            //write bsa to byte-Array c
            byte[] sharedSecret = bsa.toByteArray();

            System.out.println(" otherSecret="+byteArrayToString(otherSecret));
            //System.out.println(" otherEncapsulation="+byteArrayToString(this.serverEnsapsulated));
            return new JceTlsSecret(this.crypto, sharedSecret);
*/

        @Override
        public Pair<byte[], byte[]> keyGen() {
            System.out.println(this + " KEM: KeyGen " + this.isServer());

            // if via liboqs JNI + DLL:
            byte[] myPublicKey = kem.generate_keypair().clone();
            byte[] myPrivateKey = kem.export_secret_key().clone();


            // if pure Java (BouncyCastle):
            /*AsymmetricCipherKeyPair kp = kemGen.generateKeyPair();
            FrodoPublicKeyParameters pubParams = (FrodoPublicKeyParameters) (kp.getPublic());
            FrodoPrivateKeyParameters privParams = (FrodoPrivateKeyParameters) (kp.getPrivate());
            //variant: byte[] encoded = pubParams.getEncoded();
            //variant: byte[] encoded2 = pubParams.getPublicKey();
            this.clientPublicKey = pubParams.publicKey.clone();
            this.clientPrivateKey = privParams.getPrivateKey().clone();

            FrodoKEMGenerator gen = new FrodoKEMGenerator(this.crypto.getSecureRandom());

            SecretWithEncapsulation secEnc = gen.generateEncapsulated(pubParams);
            this.mySecret = secEnc.getSecret();
            byte[] encapsulation = secEnc.getEncapsulation();*/


            /*System.out.println(" mySecret="+byteArrayToString(mySecret));
            //System.out.println(" myEncapsulation="+byteArrayToString(encapsulation));

            byte[] mySecret2 = kem.decap_secret(encapsulation);
            System.out.println(" mySecret2="+byteArrayToString(mySecret2));

            return encapsulation;*/
            if (this.isServer()) {
                return new Pair<>(new byte[]{}, new byte[]{}); // not needed by the server
            } else {
                return new Pair<>(myPublicKey, myPrivateKey);
            }
        }

        @Override
        public Pair<byte[], byte[]> encapsulate(byte[] partnerPublicKey) {
            if (this.isServer()) {
                Pair<byte[], byte[]> pair = kem.encap_secret(partnerPublicKey);
                byte[] ciphertext = pair.getLeft();
                byte[] semiSecret = pair.getRight();
                System.out.println("SERVER SHARED SECRET: " + byteArrayToString(semiSecret));
                return new Pair<>(semiSecret, ciphertext);
            } else { // client
                return new Pair<>(new byte[]{}, new byte[]{});
            }
        }

        @Override
        public byte[] decapsulate(byte[] secretKey, byte[] ciphertext) {
            System.out.println(this + "KEM: Decapsulate");
            byte[] sharedSecret;
            if (this.isServer()) {
                sharedSecret = this.mySecret;
            } else {
                // assert: this.secretKey == secretKey
                sharedSecret = kem.decap_secret(ciphertext);
            }
            System.out.println(this + " SHARED SECRET: " + byteArrayToString(sharedSecret));

            // if via liboqs JNI + DLL:
            this.kem.dispose_KEM();
            return sharedSecret;
        }
    }

    public static class InjectableQaaSKEM extends KEMAgreementBase {

        private static final int KEY_BITS = 256;
        private final QkdProperties qkdProperties;

        public InjectableQaaSKEM(JcaTlsCrypto crypto, boolean isServer, QkdProperties qkdProperties) {
            super(crypto, isServer);
            this.qkdProperties = qkdProperties;
        }

        @Override
        public Pair<byte[], byte[]> keyGen() throws Exception {
            System.out.println("in keyGen");

            if (this.isServer()) {
                InjectedKEMs.lockKEM(0xFEFF); // User2 lock QKD KEM
                return new Pair<>(new byte[]{}, new byte[]{}); // not needed by the server
            } else {
                try {
                    InjectedKEMs.lockKEM(0xFEFF);

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

                                            byte[] fullKey = Arrays.copyOf(keyLeft.getOctets(), Math.ceilDiv(KEY_BITS, 8));
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
                    InjectedKEMs.unlockKEM(0xFEFF);
                }
            }
        }

        @Override
        public Pair<byte[], byte[]> encapsulate(byte[] partnerPublicKey) throws Exception {
            if (this.isServer()) {
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
                            if (ASN1Integer.getInstance(respSeq1.getObjectAt(2)).longValueExact() != aijaNonce+1)
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
                                        if (ASN1Integer.getInstance(respSeq2.getObjectAt(2)).longValueExact() != brencisNonce+1)
                                            throw new Exception("getKeyHalf response returned an invalid nonce");
                                        ASN1OctetString keyRight = ASN1OctetString.getInstance(respSeq2.getObjectAt(3));
                                        ASN1OctetString hashLeft2 = ASN1OctetString.getInstance(respSeq2.getObjectAt(4));
                                        ASN1ObjectIdentifier hashAlgLeft2 = ASN1ObjectIdentifier.getInstance(respSeq2.getObjectAt(5));

                                        // comparing expected hashLeft and returned hashLeft1:
                                        assert new Hash(hashLeft).equals(hashLeft2);

                                        // comparing the hashes against each other...
                                        assert new Hash(hashAlgLeft2, keyLeft).equals(hashLeft);
                                        assert new Hash(hashAlgRight1, keyRight).equals(hashRight);

                                        byte[] fullKey = Arrays.copyOf(keyLeft.getOctets(), Math.ceilDiv(KEY_BITS, 8));
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
            } else { // client
                return new Pair<>(new byte[]{}, new byte[]{});
            }
        }

        @Override
        public byte[] decapsulate(byte[] secretKey, byte[] ciphertext) throws Exception {

            if (this.isServer()) {
                InjectedKEMs.unlockKEM(0xFEFF); // User2 unlock QKD KEM
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
    }

}
