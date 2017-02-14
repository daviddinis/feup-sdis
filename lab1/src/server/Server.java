package server;

import java.io.IOException;
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
        /*    this.socket.receive(this.packet);
            processResponse(this.packet);*/
            System.out.println("boas\n");
        }
    }

    public void processResponse(DatagramPacket packet){

    }
}
