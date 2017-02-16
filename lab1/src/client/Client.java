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

    private static DatagramSocket socket;

    private static String operation;
    private static String requestString;

    private static int serverPort;

    public static void main(String[] args) throws IOException {

        if(args[2] != null){
            operation  = args[2];
            if(operation.equals("register")){
                if(args.length != 5) {
                    throw new IllegalArgumentException("\nUsage: java client.Client <host_name> <port_number> <oper> <plate_number> <owner_name>");
                }
                else {
                   checkPlateNumber(args[3]);
                    requestString = args[2] + ":" + args[3] + ":" + args[4];
                }

            }else if(operation.equals("lookup")){
                if(args.length != 4) {
                    throw new IllegalArgumentException("\nUsage: java client.Client <host_name> <port_number> <oper> <plate_number>");
                }
                else {
                    checkPlateNumber(args[3]);
                    requestString = args[2] + ":" + args[3];
                }
            }
        }
        else {
            throw new IllegalArgumentException("\nUsage: java client.Client <host_name> <port_number> <oper> <opnd>*");
        }

        //initialize some variables
        socket = new DatagramSocket(8081);
        serverPort = Integer.parseInt(args[1]);

        /*
         * send_request();
         * recv_resp();
         * process_resp();
         */

        sendRequest(requestString);

        receiveReply();
    }

    public static void receiveReply() throws IOException {
        byte[] buf = new byte[255];
        DatagramPacket packet = new DatagramPacket(buf,buf.length);

        socket.receive(packet);

        String str = new String(packet.getData(),"UTF-8");
        System.out.println(str);
    }

    public static void sendRequest(String requestString) throws IOException {

        System.out.println("Request sent!\n");
        System.out.println(requestString);

        byte[] buf = requestString.getBytes();
        InetAddress adr = InetAddress.getLocalHost();
        DatagramPacket packet = new DatagramPacket(buf,buf.length,adr,serverPort);

        socket.send(packet);

    }

    public static void checkPlateNumber(String plateNumber){

        String[] plateSubStrings = plateNumber.split("-");

        if(plateSubStrings.length != 3 || plateSubStrings[0].length() != 2 ||
                plateSubStrings[1].length() != 2 || plateSubStrings[2].length() != 2){
            throw new IllegalArgumentException("\nplate_number Usage: XX-XX-XX");
        }
    }

}
