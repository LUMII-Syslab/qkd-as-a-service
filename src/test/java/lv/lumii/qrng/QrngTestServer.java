package lv.lumii.qrng;

import lv.lumii.qkd.QkdServer;
import org.graalvm.nativeimage.c.type.CCharPointer;


public class QrngTestServer {

    public static void main(String[] args) throws Exception {

        QkdServer srv = new QkdServer();
        srv.start();

    }


}
