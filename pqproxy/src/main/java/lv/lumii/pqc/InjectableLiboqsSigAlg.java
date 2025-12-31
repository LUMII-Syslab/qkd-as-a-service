package lv.lumii.pqc;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.digests.NullDigest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.tls.DigitallySigned;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsCrypto;
import org.bouncycastle.tls.injection.sigalgs.MyMessageSigner;
import org.bouncycastle.tls.injection.sigalgs.PrivateKeyToCipherParameters;
import org.bouncycastle.tls.injection.sigalgs.PublicKeyToCipherParameters;
import org.bouncycastle.tls.injection.sigalgs.SigAlgAPI;
import org.bouncycastle.tls.injection.signaturespi.UniversalSignatureSpi;
import org.openquantumsafe.Signature;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureSpi;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class InjectableLiboqsSigAlg
        implements SigAlgAPI {
    private final String name;
    private final Collection<String> aliases;
    private final ASN1ObjectIdentifier oid;
    private final int codePoint;

    public InjectableLiboqsSigAlg(String name, Collection<String> aliases, ASN1ObjectIdentifier oid, int sigCodePoint) {
        this.name = name;
        this.aliases = aliases;
        this.oid = oid;
        this.codePoint = sigCodePoint;
    }

    public String name() {
        return this.name;
    }

    public Collection<String> aliases() {
        return this.aliases;
    }

    public ASN1ObjectIdentifier oid() {
        return this.oid;
    }

    public int codePoint() {
        return this.codePoint;
    }

    @Override
    public boolean isSupportedAlgorithm(ASN1ObjectIdentifier oid) {
        return this.oid.equals(oid);
    }

    @Override
    public boolean isSupportedParameter(AsymmetricKeyParameter someKey) {
        return someKey instanceof LiboqsParameters;
    }

    @Override
    public boolean isSupportedPublicKey(Key key) {
        return key instanceof LiboqsPublicKey;
    }

    @Override
    public boolean isSupportedPrivateKey(Key key) {
        return key instanceof LiboqsPrivateKey;
    }

    @Override
    public AsymmetricKeyParameter createPrivateKeyParameter(PrivateKeyInfo keyInfo) throws IOException {
        int len = keyInfo.getPrivateKey().getOctetsLength();
        byte[] sk = keyInfo.getPrivateKey().getOctets();
        byte[] skEnc = keyInfo.getPrivateKey().getEncoded();
        //byte[] skEnc = ASN1OctetString.getInstance(keyInfo.parsePrivateKey()).getOctets();

        byte[] pk = null;
        byte[] pkEnc = null;
        if (keyInfo.hasPublicKey()) {
            pk = keyInfo.getPublicKeyData().getOctets();
            pkEnc = keyInfo.getPublicKeyData().getEncoded();
            //pkEnc = ASN1OctetString.getInstance(keyInfo.parsePublicKey()).getOctets();
        }
        return new LiboqsParameters(true, pk, pkEnc, sk, skEnc);
    }

    @Override
    public PrivateKeyInfo createPrivateKeyInfo(AsymmetricKeyParameter privateKey, ASN1Set attributes) throws IOException {
        LiboqsParameters params = (LiboqsParameters) privateKey;

        AlgorithmIdentifier algorithmIdentifier =
                new AlgorithmIdentifier(oid);
        return new PrivateKeyInfo(algorithmIdentifier, new DEROctetString(params.skEncoded()), attributes, params.pkEncoded());
    }

    @Override
    public AsymmetricKeyParameter createPublicKeyParameter(SubjectPublicKeyInfo keyInfo, Object defaultParams) throws IOException {
        byte[] wrapped = keyInfo.getEncoded(); // ASN1 wrapped
        SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(wrapped);
        byte[] pk = info.getPublicKeyData().getOctets();
        byte[] pkEnc = info.getPublicKeyData().getEncoded();

        //AlgorithmIdentifier alg = keyInfo.getAlgorithm(); ??
        return new LiboqsParameters(false, pk, pkEnc, null, null);
    }

    @Override
    public SubjectPublicKeyInfo createSubjectPublicKeyInfo(AsymmetricKeyParameter publicKey) throws IOException {
        LiboqsParameters params = (LiboqsParameters) publicKey;

        byte[] encoding = params.pkEncoded();

        // remove the first 4 bytes (alg. params)
        //if (encoding.length == sphincsPlusPKLength+4)
        //  encoding = Arrays.copyOfRange(encoding, 4, encoding.length);

        AlgorithmIdentifier algorithmIdentifier = new AlgorithmIdentifier(oid);//??? -- does not matter
        // new AlgorithmIdentifier(Utils.sphincsPlusOidLookup(params.getParameters())); // by SK: here BC gets its algID!!!
        return new SubjectPublicKeyInfo(algorithmIdentifier, new DEROctetString(encoding));
    }

    @Override
    public PrivateKey generatePrivate(PrivateKeyInfo keyInfo) throws IOException {

        int len = keyInfo.getPrivateKey().getOctetsLength();
        byte[] sk = keyInfo.getPrivateKey().getOctets();
        byte[] skEncoded = keyInfo.getEncoded();

        byte[] pk = null;
        byte[] pkEncoded = null;
        ASN1BitString pubData = keyInfo.getPublicKeyData();
        if (pubData != null) {
            pk = pubData.getOctets();
            pkEncoded = pubData.getEncoded();
        }

        return new LiboqsPrivateKey(name, pk, pkEncoded, sk, skEncoded);
    }

    @Override
    public PublicKey generatePublic(SubjectPublicKeyInfo keyInfo) throws IOException {
        byte[] pk = keyInfo.getPublicKeyData().getOctets();
        byte[] pkEncoded = keyInfo.getPublicKeyData().getEncoded();

        return new LiboqsPublicKey(name, pk, pkEncoded);
    }

    @Override
    public byte[] internalEncodingFor(PublicKey key) {
        if (key instanceof LiboqsPublicKey)
            return ((LiboqsPublicKey) key).pk();
        else
            throw new RuntimeException("Not a LiboqsPublicKey given.");
    }

    @Override
    public byte[] internalEncodingFor(PrivateKey key) {
        if (key instanceof LiboqsPrivateKey)
            return ((LiboqsPrivateKey) key).sk();
        else
            throw new RuntimeException("Not a LiboqsPrivateKey given.");
    }

    @Override
    public byte[] sign(JcaTlsCrypto crypto, byte[] message, byte[] privateKey) throws IOException {
        Signature signer = new Signature(name, privateKey);

        if (privateKey.length > signer.secret_key_length()) {
            // liboqs stores private keys as follows: 4 (ASN octet string), <len>, private key, public key
            privateKey = Arrays.copyOfRange(privateKey, (int)privateKey.length-(int)signer.secret_key_length()-(int)signer.public_key_length(), (int)privateKey.length-(int)signer.public_key_length());
            signer = new Signature(name, privateKey);
        }
        byte[] signature = signer.sign(message);
        return signature;
    }

    @Override
    public boolean verifySignature(byte[] message, byte[] publicKey, DigitallySigned signature) {
        Signature verifier = new Signature(name);
        boolean isValid = verifier.verify(message, signature.getSignature(), publicKey);
        return isValid;
    }

    @Override
    public SignatureSpi signatureSpi(Key publicOrPrivateKey) {
        boolean nameMatches = name.equals(publicOrPrivateKey.getAlgorithm());
        for (String alias : this.aliases)
            nameMatches = nameMatches || alias.equals(publicOrPrivateKey.getAlgorithm());

        if (nameMatches && !(publicOrPrivateKey instanceof LiboqsPublicKey)) {
            publicOrPrivateKey = new LiboqsPublicKey(name, null, publicOrPrivateKey.getEncoded());
        }

        if (publicOrPrivateKey instanceof LiboqsPublicKey) {
            PublicKeyToCipherParameters f1 = (pk) -> {
                // if (name.equals(pk.getAlgorithm()))...
                if (pk instanceof LiboqsPublicKey) {
                    return new LiboqsParameters(false,
                            ((LiboqsPublicKey) pk).pk(), ((LiboqsPublicKey) pk).getEncoded(), null, null);
                } else
                    throw new RuntimeException("Not a LiboqsPublicKey given.");

            };
            PrivateKeyToCipherParameters f2 = (sk) -> {
                if (sk instanceof LiboqsPrivateKey)
                    return new LiboqsParameters(true,
                            ((LiboqsPrivateKey) sk).pk(), ((LiboqsPrivateKey) sk).pkEncoded(),
                            ((LiboqsPrivateKey) sk).sk(), ((LiboqsPrivateKey) sk).skEncoded());
                else
                    throw new RuntimeException("Not a LiboqsPrivateKey given.");
            };

            return new UniversalSignatureSpi(new NullDigest(),
                    new MyMessageSigner(
                            codePoint,
                            this::sign,
                            this::verifySignature,
                            (params) -> {
                                assert params instanceof LiboqsParameters;
                                LiboqsParameters pkParams = (LiboqsParameters) params;
                                return pkParams.pk();
                            },
                            (params) -> {
                                assert params instanceof LiboqsParameters;
                                LiboqsParameters skParams = (LiboqsParameters) params;
                                return skParams.sk();
                            }),
                    f1, f2);

        } else
            throw new RuntimeException("Only " + name + " is supported in this implementation of SignatureSpi");
    }

}
