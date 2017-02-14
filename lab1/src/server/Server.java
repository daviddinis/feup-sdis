package server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by epassos on 2/14/17.
 */
public class Server {

    private int portNumber;
    private int length;
    private byte[] buf;
    private DatagramSocket socket;
    private DatagramPacket packet;

    Server(int port)throws IOException {
        this.portNumber = port;

        this.length = 100;
        this.buf = new byte[length];

        this.socket = new DatagramSocket(portNumber);
        this.packet = new DatagramPacket(buf, length);
    }

    public void running()throws IOException {
        /**
         * recv_request();
         * process_req();
         * send_resp();
         */

        while(true){
            this.socket.receive(this.packet);
            processResponse(this.packet);
        }
    }

    public void processResponse(DatagramPacket packet) throws UnsupportedEncodingException {
        System.out.println("New Request");
        byte[] buf = packet.getData();
        String str = new String(buf,"UTF-8");
        System.out.println(str);
    }
}
