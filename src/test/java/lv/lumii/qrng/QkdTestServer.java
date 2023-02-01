package lv.lumii.qrng;

import lv.lumii.qkd.QkdServer;


public class QkdTestServer {

    public static void main(String[] args) throws Exception {

        QkdServer srv = new QkdServer();
        srv.start();

    }


}
