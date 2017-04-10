package cli;

import common.InitiatorInterface;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientInterface {

    private static InitiatorInterface initiatorPeer;

    public static void main(String args[]) throws IOException, NotBoundException {

        if (args.length == 0) {
            System.out.println(args.length);
            throw new IllegalArgumentException("\nUsage: java ClientInterface <peerAp>" +
                    " <sub_protocol> <opnd1> <opnd2> ");
        }

        String peerAp = args[0];
        String operation = args[1];

        try {
            Registry registry = LocateRegistry.getRegistry();
            initiatorPeer = (InitiatorInterface) registry.lookup(peerAp);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }

        String filepath;
        int replicationDegree;
        int maxDiskSpace;

        try {
            switch (operation.toUpperCase()) {
               case "BACKUP":
                    filepath = args[2];
                    replicationDegree = Integer.parseInt(args[3]);
                    System.out.println("Requesting backup of file " + filepath + " with a replication degree of " + replicationDegree);
                    initiatorPeer.backup(filepath, replicationDegree);
                    break;
                case "RESTORE":
                    filepath = args[2];
                    System.out.println("Requesting restore of file" + filepath);
                    initiatorPeer.restore(filepath);
                    break;
                case "DELETE":
                    filepath = args[2];
                    System.out.println("Requesting deletion of file" + filepath);
                    initiatorPeer.delete(filepath);
                    break;
                case "RECLAIM":
                    maxDiskSpace = Integer.parseInt(args[2]);
                    System.out.println("Updating maximum available space to " + maxDiskSpace);
                    initiatorPeer.reclaim(maxDiskSpace);
                    break;
                case "STATE":
                    System.out.println(initiatorPeer.state());
                    break;
                default:
                    break;
            }
        }catch (IllegalArgumentException e){
            System.err.println(e.getMessage());
        }

    }
}
