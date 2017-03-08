package client;

import server.Server;

import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by epassos on 2/20/17.
 */
public class Client {
    private static final String plateRegex = "^([0-9A-Za-z])([0-9A-Za-z])-([0-9A-Za-z])([0-9A-Za-z])-([0-9A-Za-z])([0-9A-Za-z])$";
    public static int port = 8085;

    public static void main(String args[]) throws IOException {
        if(args.length < 4){
            System.out.println("Invalid number of arguments");
            System.out.println("Usage:");
            System.out.println("Client <hostname> <port> <oper> <opnd>*");
            System.exit(1);
        }

        String hostname = args[0];
        String port = args[1];
        String operation = args[2];
        String plate = args[3];


        Pattern platePattern = Pattern.compile(plateRegex);
        Matcher plateMatcher = platePattern.matcher(plate);
        if(!plateMatcher.find()){
            System.out.println("Invalid format for license plate");
            System.exit(1);
        }

        Socket socket = new Socket(hostname, Integer.parseInt(port));
        PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );

        if(operation.equalsIgnoreCase("register")){
            String name = args[4];
            for(int i = 3; i < args.length; i++){
                name += ' ' + args[i];

                if(name.length() > 256) {
                    name = name.substring(0, 256);
                    break;
                }
            }

            sendRegisterRequest(plate, name,  out);
        }

        else if(operation.equalsIgnoreCase("lookup"))
            sendLookupRequest(plate, out);

        else{
            System.err.println("Invalid operation.");
            System.exit(1);
        }

        String resp = receiveResponse(in);

        System.out.println(resp);
        socket.close();
    }

    private static int sendRegisterRequest(String plate, String name, PrintWriter out) throws IOException {
        String req = "register" + ';' + plate + ';' + name;
        out.println(req);
        System.out.println(req);
        return 0;
    }

    private static int sendLookupRequest(String plate, PrintWriter out) throws  IOException {
        String req = "lookup" + ';' + plate;
        out.println(req);
        System.out.println(req);
        return 0;
    }

    private static String receiveResponse(BufferedReader in) throws IOException {
        String response = in.readLine().trim();

        return response;
    }

}

