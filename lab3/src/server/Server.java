package server;


import client.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashMap;

/**
 * Created by epassos on 2/20/17.
 */
public class Server {
    private static HashMap<String,String> cars;

    public static void main(String args[]) throws IOException {
        if(args.length != 1){
            System.out.println("Invalid number of arguments");
            System.out.println("Usage:");
            System.out.println("Server <srvc_port>");
            System.exit(1);
        }
        int svPort = Integer.parseInt(args[0]);
        ServerSocket svSocket = new ServerSocket(svPort);

        cars = new HashMap<>();

        try {
            getRequest(svSocket);
        }catch(IOException e){
            System.err.println("Socket error");
            System.exit(1);
        }
    }

    private static void getRequest(ServerSocket svSocket) throws IOException {

        while(true) {
            try {
                Socket clientSocket = svSocket.accept();


                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream())
                );

                String inputLine;
                String request = "";

                while((inputLine = in.readLine().trim()) != null){
                    request += inputLine;
                    System.out.println(request);
                    String response = processRequest(request.trim());
                    System.out.println(response);
                    out.println(response);
                    break;
                }
            } catch (IOException e) {

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

//    private static void reply(String response, ServerSocket svSocket, Socket clientSocket, PrintWriter out) throws IOException {
//        byte[] buf = response.getBytes();
//        DatagramPacket packet = new DatagramPacket(buf, buf.length, replyAddr, replyPort);
//        socket.send(packet);
//    }
}
