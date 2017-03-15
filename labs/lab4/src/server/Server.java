package server;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class Server implements RemoteInterface{
    private static HashMap<String, String> cars;

    public static final void main(String args[]){
        cars = new HashMap<String, String>();
        Server serverObj = new Server();
        try {
            RemoteInterface remoteObject = (RemoteInterface) UnicastRemoteObject.exportObject(serverObj,0);
            Registry rmiReg = LocateRegistry.getRegistry();
            rmiReg.bind("carManager",remoteObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public String register(String plate, String name){
        String response;
        if(cars.containsKey(plate)){
            System.out.println("Plate already registered");
            response =  "-1";
        }
        else {
            System.out.println("Registering plate " + plate + " to " + name);
            cars.put(plate, name);
            response = Integer.toString(cars.size());
        }

        return response;
    }

    @Override
    public String lookup(String plate){
        String response;
        if(!cars.containsKey(plate)){
            System.out.println("Plate not registered");
            response = "NOT_FOUND";
        }
        else {
            System.out.println("Lookup for plate " + plate);
            response =  cars.get(plate);
        }

        return response;
    }
}