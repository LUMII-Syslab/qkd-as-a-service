package lv.lumii.pqc;

import java.security.PublicKey;

public class LiboqsPublicKey implements PublicKey
{
    private String name;
    private byte[] pk, pkEncoded;
    public LiboqsPublicKey(String name, byte[] pk, byte[] pkEncoded) {
        this.name = name;
        this.pk = pk;
        this.pkEncoded = pkEncoded;
    }
    @Override
    public String getAlgorithm()
    {
        return name;
    }

    @Override
    public String getFormat()
    {
        return "X.509";
    }

    public byte[] pk() {
        return this.pk;
    }

    @Override
    public byte[] getEncoded()
    {
        return this.pkEncoded;
    }
}
