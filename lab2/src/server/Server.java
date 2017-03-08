package server;


import client.Client;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;

/**
 * Created by epassos on 2/20/17.
 */
public class Server {
    private static HashMap<String,String> cars;

    public static void main(String args[]) throws IOException {
        if(args.length != 3){
            System.out.println("Invalid number of arguments");
            System.out.println("Usage:");
            System.out.println("Server <srvc_port> <mcast_addr> <mcast_port>");
            System.exit(1);
        }
        String svPort = args[0];
        String mcastAddress = args[1];
        String mcastPort = args[2];

        MulticastSocket mcastSocket = new MulticastSocket(Integer.parseInt(mcastPort));
        DatagramSocket svSocket  = new DatagramSocket(Integer.parseInt(svPort));
        cars = new HashMap<>();

        try {
            getRequest(svSocket, svPort, mcastSocket, mcastAddress, mcastPort);
        }catch(IOException e){
            System.err.println("Socket error");
            System.exit(1);
        }
    }

    private static void getRequest(DatagramSocket svSocket, String svPort, MulticastSocket mcastSocket, String mcastAddr, String mcastPort) throws IOException {
        InetAddress mcastGroup = InetAddress.getByName(mcastAddr);
        svSocket.setSoTimeout(1500);  //timeout to send a multicast

        byte[] publicPacket = svPort.getBytes();
        DatagramPacket addrPacket = new DatagramPacket(publicPacket,publicPacket.length,mcastGroup,Integer.parseInt(mcastPort));
        mcastSocket.send(addrPacket);

        while(true) {
            byte[] buf = new byte[512];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                svSocket.receive(packet);
                InetAddress replyAddr = packet.getAddress();
                int replyPort = packet.getPort();
                String request = new String(packet.getData());
                String response = processRequest(request.trim());
                reply(response, svSocket, replyAddr, replyPort);
            } catch (IOException e) {
                mcastSocket.send(addrPacket); //when the server's service socket times out, the multicast socket sends the information again
            }
        }
    }

    private static String processRequest(String request){
        String[] reqArgs = request.split(";");
        String operation = reqArgs[0];
        String plate = reqArgs[1];
        String response;

        if(operation.equalsIgnoreCase("register")) {
            String name = reqArgs[2];
            if(cars.containsKey(plate)){
                System.out.println("Plate already registered");
                response =  "-1";
            }
            else {
                System.out.println("Registering plate " + plate + " to " + name);
                cars.put(plate, name);
                response = Integer.toString(cars.size());
            }
        }
        else if(operation.equalsIgnoreCase("lookup")){
            if(!cars.containsKey(plate)){
                System.out.println("Plate not registered");
                response = "NOT_FOUND";
            }
            else {
                System.out.println("Lookup for plate " + reqArgs[1]);
                response =  cars.get(plate);
            }
        }
        else{
            System.out.println("invalid operation!");
            response = "-1";
        }

        return response;
    }

    private static void reply(String response, DatagramSocket socket, InetAddress replyAddr, int replyPort) throws IOException {
        byte[] buf = response.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, replyAddr, replyPort);
        socket.send(packet);
    }
}
