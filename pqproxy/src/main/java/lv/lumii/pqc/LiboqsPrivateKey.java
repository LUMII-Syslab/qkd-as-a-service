package lv.lumii.pqc;

import java.security.PrivateKey;

public class LiboqsPrivateKey
        implements PrivateKey
{
    private String name;
    private byte[] pk, pkEncoded;
    private byte[] sk, skEncoded;

    public LiboqsPrivateKey(String name, byte[] pk, byte[] pkEncoded, byte[] sk, byte[] skEncoded) {
        this.name = name;
        this.pk = pk;
        this.pkEncoded = pkEncoded;
        this.sk = sk;
        this.skEncoded = skEncoded;
    }

    @Override
    public String getAlgorithm()
    {
        return name;
    }

    @Override
    public String getFormat()
    {
        return "PKCS#8";
    }

    @Override
    public byte[] getEncoded()
    {
        return this.skEncoded;
    }

    public byte[] pk() {
        return this.pk;
    }

    public byte[] pkEncoded() {
        return this.pkEncoded;
    }

    public byte[] sk() {
        return this.sk;
    }

    public byte[] skEncoded() {
        return this.skEncoded;
    }
}
