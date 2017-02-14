package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by epassos on 2/14/17.
 */
public class Client {
    public static void main(String[] args) throws IOException {
        if(args.length < 2){
            System.out.println("Wrong Number of Arguments");
            System.exit(1);
        }

        DatagramSocket socket = new DatagramSocket(8081);
        String operation  = args[0];
        if(operation.equals("register")){
            if(args.length != 3) {
                System.out.println("Wrong Number of Arguments");
                System.exit(1);
            }


        }else if(operation.equals("lookup")){
            if(args.length != 2) {
                System.out.println("Wrong Number of Arguments");
                System.exit(1);
            }
        }

        String str = "Boas";
        byte[] buf = str.getBytes();
        InetAddress adr = InetAddress.getLocalHost();

        DatagramPacket packet = new DatagramPacket(buf,buf.length,adr,8080);
        /*
         * send_request();
         * recv_resp();
         * process_resp();
         */
        socket.send(packet);
    }
}
