package src.cli;

import com.sun.org.apache.xml.internal.security.Init;
import src.common.InitiatorInterface;
import src.peers.PeerService;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by nuno on 09-03-2017.
 */
public class ClientInterface {

    private static InitiatorInterface initiatorPeer;

    public static void main(String args[]) throws RemoteException, NotBoundException {

        try {
            Registry registry = LocateRegistry.getRegistry();
            initiatorPeer  = (InitiatorInterface) registry.lookup("teste");
        } catch (Exception e){
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
        initiatorPeer.backup("fwfwe",2);
    }
}
