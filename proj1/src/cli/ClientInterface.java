package cli;

import common.InitiatorInterface;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientInterface {

    private static InitiatorInterface initiatorPeer;

    public static void main(String args[]) throws IOException, NotBoundException {

        if (args.length != 4 && args.length != 5) {
            System.out.println(args.length);
            throw new IllegalArgumentException("\nUsage: java ClientInterface <peerAp>" +
                    " <protocol-version> <operation> <opnd1> <opnd2> ");
        }

        String peerAp = args[0];

        String operation = args[2];

        try {
            Registry registry = LocateRegistry.getRegistry();
            initiatorPeer = (InitiatorInterface) registry.lookup(peerAp);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }

        String pathname;

        try {
            switch (operation.toUpperCase()) {
                case "BACKUP":
                    pathname = args[3];
                    int replicationDegree = Integer.parseInt(args[4]);
                    System.out.println("Requesting backup of file " + pathname + " with a replication degree of " + replicationDegree);
                    initiatorPeer.backup(pathname, replicationDegree);
                    break;
                case "RESTORE":
                    pathname = args[3];
                    System.out.println("Requesting restore of file" + pathname);
                    initiatorPeer.restore(pathname);
                    break;
                case "DELETE":
                    pathname = args[3];
                    System.out.println("Requesting deletion of file" + pathname);
                    initiatorPeer.delete(pathname);
                    break;
                case "RECLAIM":
                    int maxDiskSpace = Integer.parseInt(args[3]);
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
