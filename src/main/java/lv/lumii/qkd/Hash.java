package lv.lumii.qkd;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHAKEDigest;
import org.cactoos.Scalar;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Unchecked;

import java.util.Arrays;

public class Hash {


    Unchecked<byte[]> hash;

    public Hash(ASN1ObjectIdentifier digestOid, byte[] data) throws Exception {
        this(digestOid.toString(), data);
    }

    public Hash(ASN1ObjectIdentifier digestOid, ASN1OctetString data) throws Exception {
        this(digestOid.toString(), data.getOctets());
    }

    public Hash(String digestOid, byte[] data) throws Exception {

        digestOid = digestOid.toLowerCase().replace("-", "");

        Digest d;
        if ("2.16.840.1.101.3.4.2.11".equals(digestOid) || "2.16.840.1.101.3.4.2.17".equals(digestOid)  || "shake128".equals(digestOid))
            d = new SHAKEDigest(128);
        else
        if ("2.16.840.1.101.3.4.2.1".equals(digestOid) || "sha256".equals(digestOid))
            d = new SHA256Digest();
        else
            throw new Exception("Unsupported key hash algorithm "+digestOid);

        this.hash = new Unchecked<>(new Sticky<>(()-> compute(d, data)));
    }

    public Hash(ASN1OctetString hash) {
        this.hash = new Unchecked<>(()->hash.getOctets());
    }

    private byte[] compute(Digest d, byte[] data) {
        d.update(data, 0, data.length);
        byte[] hash = new byte[d.getDigestSize()];
        d.doFinal(hash, 0);
        return hash;
    }

    public byte[] value() {
        return this.hash.value();
    }

    @Override
    public boolean equals(Object o) {

        if (o instanceof byte[]) {
            return Arrays.equals(this.hash.value(), (byte[])o);
        }
        if (o instanceof ASN1OctetString) {
            return Arrays.equals(this.hash.value(), ((ASN1OctetString)o).getOctets());
        }

        return super.equals(o);
    }
}
