package lv.lumii.qkd;

import lv.lumii.pqc.InjectableFrodoKEM;
import lv.lumii.pqc.InjectableSphincsPlus;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.tls.injection.InjectableAlgorithms;
import org.bouncycastle.tls.injection.InjectableKEMs;
import org.bouncycastle.tls.injection.InjectionPoint;

import java.security.Security;

/**
 * The class for injecting QaaS KEM and some PQC algorithms used in it
 * <p>
 *
 * @author Sergejs Kozlovics
 */
public class InjectableQKD {


    public static void inject(QkdProperties qkdProperties) {
        // PQC signatures are huge; increasing the max handshake size:
        System.setProperty("jdk.tls.maxHandshakeMessageSize", String.valueOf(32768 * 32));

        InjectableSphincsPlus mySphincs = new InjectableSphincsPlus();

        InjectableAlgorithms pqcAlgs = new InjectableAlgorithms()
                .withSigAlg(mySphincs.name(), mySphincs.oid(), mySphincs.codePoint(), mySphincs)
                .withKEM(InjectableFrodoKEM.NAME, InjectableFrodoKEM.CODE_POINT,
                        InjectableFrodoKEM::new, InjectableKEMs.Ordering.BEFORE);
        pqcAlgs = pqcAlgs.withoutDefaultKEMs();

        InjectionPoint injectionPoint = InjectionPoint._new();
        injectionPoint.push(pqcAlgs);

        InjectableQaasKEM qaasKEM = new InjectableQaasKEM(qkdProperties, injectionPoint, pqcAlgs);
        qaasKEM.inject(); // will inject after pqcAlgs

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


}
