package lv.lumii.test;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;

public class ASN1Test {
    public static String byteArrayToString(byte[] a) {
        return byteArrayToString(a, "");
    }
    public static String byteArrayToString(byte[] a, String delim) {
        String s = "";
        for (byte b : a) {
            if (s.length()>0)
                s += delim;
            s += String.format("%02x ", b);
        }
        return s;
    }
    public static void main(String[] args) throws Exception {

        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new ASN1Integer(1)); // endpoint (function) id
        v.add(new ASN1Integer(256)); // key length
        v.add(new ASN1Integer(12345)); // nonce

        DERSequence seq = new DERSequence(v);
        System.out.println("reserveKeyAndGetHalf: "+byteArrayToString(seq.getEncoded()));

    }


}
