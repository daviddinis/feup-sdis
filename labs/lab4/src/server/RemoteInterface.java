package server;

import java.rmi.*;

public interface RemoteInterface extends Remote {
   String register(String plate, String name) throws RemoteException;
   String lookup(String plate) throws RemoteException;
}