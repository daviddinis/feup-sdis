package client;

import server.Server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.regex.*;

/**
 * Created by epassos on 3/14/17.
 */
public class Client {

    private static final String plateRegex = "^([0-9A-Za-z])([0-9A-Za-z])-([0-9A-Za-z])([0-9A-Za-z])-([0-9A-Za-z])([0-9A-Za-z])$";
    public static void main(String args[]){
        if(args.length < 2){
            System.out.println("Invalid num of arguments");
            System.exit(1);
        }
        String operation = args[0];
        String plate = args[1];

        try {
            Registry registry = LocateRegistry.getRegistry();
            Server server = (Server) registry.lookup("carManager");

            Pattern platePattern = Pattern.compile(plateRegex);
            Matcher plateMatcher = platePattern.matcher(plate);
            if (!plateMatcher.find()) {
                System.out.println("Invalid format for license plate");
                System.exit(1);
            }

            if (operation.equalsIgnoreCase("register")) {
                String name = args[2];
                for (int i = 3; i < args.length; i++) {
                    name += ' ' + args[i];

                    if (name.length() > 256) {
                        name = name.substring(0, 256);
                        break;
                    }
                }

                System.out.println(server.register(plate, name));
            } else if (operation.equalsIgnoreCase("lookup"))
                System.out.println(server.lookup(plate));

            else {
                System.err.println("Invalid operation.");
                System.exit(1);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
