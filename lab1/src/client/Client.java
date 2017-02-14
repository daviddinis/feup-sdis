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
        /*if(args.length != 4 && args.length != 5){
            throw new IllegalArgumentException("\nUsage: java client.Client <host_name> <port_number> <oper> <opnd>*");
        }*/

        String str = new String();

        if(args[2] != null){
            String operation  = args[2];
            if(operation.equals("register")){
                if(args.length != 5) {
                    throw new IllegalArgumentException("\nUsage: java client.Client <host_name> <port_number> <oper> <plate_number> <owner_name>");
                }
                else {
                 str = args[3] + " " + args[4];
                }

            }else if(operation.equals("lookup")){
                if(args.length != 4) {
                    throw new IllegalArgumentException("\nUsage: java client.Client <host_name> <port_number> <oper> <plate_number>");
                }
                else {
                    str = args[3];
                }
            }
        }
        else {
            throw new IllegalArgumentException("\nUsage: java client.Client <host_name> <port_number> <oper> <opnd>*");
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
        DatagramSocket socket = new DatagramSocket(8081);

        socket.send(packet);
    }

    public static String getStringRegister(String arg1, String arg2){

        String ret = arg1+ " " + arg2;
    }
}
