package src.peers;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class PeerLauncher {
    public static void main(String[] args) throws UnknownHostException {
        if(args.length != 6){
            throw new IllegalArgumentException("\nUsage: java PeerLauncher <mc_addr> <mc_port> <mdb_addr> <mdb_port>" +
                    " <mdr_addr> <mdr_port>");
        }

        InetAddress mc_addr = InetAddress.getByName(args[0]);
        int mc_port = Integer.parseInt(args[1]);
        InetAddress mdb_addr = InetAddress.getByName(args[2]);
        int mdb_port = Integer.parseInt(args[3]);
        InetAddress mdr_addr = InetAddress.getByName(args[4]);
        int mdr_port = Integer.parseInt(args[5]);

        PeerService peerService = new PeerService(mc_addr,mc_port,mdb_addr,mdb_port,mdr_addr,mdr_port);
    }
}
