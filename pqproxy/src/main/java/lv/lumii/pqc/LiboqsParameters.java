package lv.lumii.pqc;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

public class LiboqsParameters
        extends AsymmetricKeyParameter
{
    private byte[] pk, pkEnc, sk, skEnc;
    public LiboqsParameters(boolean privateKey, byte[] pk, byte[] pkEnc, byte[] sk, byte[] skEnc)
    {
        super(privateKey);
        this.pk = pk;
        this.pkEnc = pkEnc;
        this.sk = sk;
        this.skEnc = skEnc;
    }

    public byte[] pk() {
        return this.pk;
    }

    public byte[] sk() {
        return this.sk;
    }

    public byte[] pkEncoded() {
        return this.pkEnc;
    }

    public byte[] skEncoded() {
        return this.skEnc;
    }
}
