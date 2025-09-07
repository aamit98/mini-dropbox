package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Server;

public class TftpServer {
    public static void main(String[] args) {
        int port = 7777;
        if (args != null && args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (Exception ignore) {}
        }
        // run server
        Server.threadPerClient(
                port,
                TftpProtocol::new,
                TftpEncoderDecoder::new
        ).serve();
    }
}
