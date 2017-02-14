package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by epassos on 2/14/17.
 */
public class Server {
    public static void main(String[] args) throws IOException {
        /**
         * recv_request();
         * process_req();
         * send_resp();
         */

        int length = 100;
        byte[] buf = new byte[length];

        DatagramSocket socket = new DatagramSocket(8080);
        DatagramPacket packet = new DatagramPacket(buf, length);

        while(true){
            socket.receive(packet);
            this.processResponse(packet);
        }
    }

    public void processResponse(DatagramPacket packet){

    }
}
