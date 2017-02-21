package server;


import client.Client;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;

/**
 * Created by epassos on 2/20/17.
 */
public class Server {
    public static final int port = 8080;
    private static HashMap<String,String> cars;

    public static void main(String args[]) throws SocketException, UnknownHostException {
        DatagramSocket socket = new DatagramSocket(port);
        cars = new HashMap<>();

        try {
            getRequest(socket);
        }catch(IOException e){
            System.err.println("Socket error");
            System.exit(1);
        }
    }

    private static void getRequest(DatagramSocket socket) throws IOException {
        InetAddress address = InetAddress.getLocalHost();

        while(true) {
            byte[] buf = new byte[512];
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
            try {
                socket.receive(packet);
                String request = new String(packet.getData());
                String response = processRequest(request.trim());
                reply(response, socket);
            } catch (IOException e) {
                System.out.println("Socket error");
                throw new IOException("Socket error");
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
            System.out.println(plate);
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

    private static void reply(String response, DatagramSocket socket) throws IOException {
        byte[] buf = response.getBytes();
        InetAddress address = InetAddress.getLocalHost();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, Client.port);
        socket.send(packet);
    }
}
