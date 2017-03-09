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

        if(args.length != 4 && args.length != 5){
            throw new IllegalArgumentException("\nUsage: java ClientInterface <peerAp>" +
                    " <subProtocol> <opnd1> <opnd2> ");
        }

        String peerAp = args[0];
        String subPrototocol = args[1];
        String operation = args[2];

        try {
            Registry registry = LocateRegistry.getRegistry();
            initiatorPeer  = (InitiatorInterface) registry.lookup(peerAp);
        } catch (Exception e){
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }

        String pathname;

        switch (operation){
            case "BACKUP":
                pathname = args[3];
                int replicationDegree = Integer.parseInt(args[4]);
                initiatorPeer.backup(pathname,replicationDegree);
                break;
            case "RESTORE":
                pathname = args[3];
                initiatorPeer.restore(pathname);
                break;
            case "DELETE":
                pathname = args[3];
                initiatorPeer.delete(pathname);
                break;
            case "RECLAIM":
                int maxDiskSpace = Integer.parseInt(args[3]);
                initiatorPeer.reclaim(maxDiskSpace);
                break;
            case "STATE":
                initiatorPeer.state();
                break;
            default:
                break;
        }

    }
}
