package client;

import server.Server;

import java.io.IOException;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by epassos on 2/20/17.
 */
public class Client {
    private static final String plateRegex = "^([0-9A-Za-z])([0-9A-Za-z])-([0-9A-Za-z])([0-9A-Za-z])-([0-9A-Za-z])([0-9A-Za-z])$";
    public static int port = 8081;

    public static void main(String args[]) throws IOException {
        if(args.length < 2){
            System.out.println("Invalid number of arguments");
            System.exit(1);
        }

        DatagramSocket socket = new DatagramSocket(port);
        socket.setSoTimeout(2500);
        String operation = args[0];
        String plate = args[1];

        Pattern platePattern = Pattern.compile(plateRegex);
        Matcher plateMatcher = platePattern.matcher(plate);
        if(!plateMatcher.find()){
            System.out.println("Invalid format for license plate");
            System.exit(1);
        }

        if(operation.equalsIgnoreCase("register")){
            String name = args[2];
            for(int i = 3; i < args.length; i++){
                name += ' ' + args[i];

                if(name.length() > 256) {
                    name = name.substring(0, 256);
                    break;
                }
            }

            sendRegisterRequest(plate, name, socket);
        }

        else if(operation.equalsIgnoreCase("lookup"))
            sendLookupRequest(plate, socket);

        else{
            System.err.println("Invalid operation.");
            System.exit(1);
        }

        String resp = receiveResponse(socket);

        System.out.println(resp);
        socket.close();
    }

    private static int sendRegisterRequest(String plate, String name, DatagramSocket socket) throws IOException {
        InetAddress address = InetAddress.getLocalHost();
        String req = "register" + ';' + plate + ';' + name;
        byte[] buf = req.getBytes();
        DatagramPacket reqPacket = new DatagramPacket(buf,buf.length,address,Server.port);
        try {
            socket.send(reqPacket);
        }catch (IOException e){
            throw new IOException("Error sending lookup request");
        }
        return 0;
    }

    private static int sendLookupRequest(String plate, DatagramSocket socket) throws  IOException {
        InetAddress address = InetAddress.getLocalHost();
        String req = "lookup" + ';' + plate;
        System.out.println(plate);
        byte[] buf = req.getBytes();
        DatagramPacket reqPacket = new DatagramPacket(buf, buf.length, address, Server.port);
        try {
            socket.send(reqPacket);
        }catch (IOException e){
            throw new IOException("Error sending lookup request");
        }
        return 0;
    }

    private static String receiveResponse(DatagramSocket socket) throws IOException {
        byte[] buf = new byte[512];
        InetAddress address = InetAddress.getLocalHost();
        DatagramPacket packet = new DatagramPacket(buf,buf.length,address,Server.port);
        socket.receive(packet);
        return new String(packet.getData());
    }
}
