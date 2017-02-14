package client;

import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by epassos on 2/14/17.
 */
public class Client {
    public static void main(String[] args) throws SocketException {
        if(args.length < 2){
            System.out.println("Wrong Number of Arguments");
            System.exit(1);
        }

        DatagramSocket socket = new DatagramSocket(8080);
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


        /*
         * send_request();
         * recv_resp();
         * process_resp();
         */
    }
}
