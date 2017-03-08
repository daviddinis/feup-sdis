package src.peers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class PeerLauncher {
    public static void main(String[] args) throws IOException {
        if(args.length != 7){
            throw new IllegalArgumentException("\nUsage: java PeerLauncher <peerId> <mcAddr> <mcPort> " +
                    "<mdbSddr> <mdbPort> <mdrAddr> <mdrPort>");
        }

        String serverId = args[0];
        InetAddress mcAddr = InetAddress.getByName(args[1]);
        int mcPort = Integer.parseInt(args[2]);
        InetAddress mdbAddr = InetAddress.getByName(args[3]);
        int mdbPort = Integer.parseInt(args[4]);
        InetAddress mdrAddr = InetAddress.getByName(args[5]);
        int mdrPort = Integer.parseInt(args[6]);

        PeerService peerService = new PeerService(serverId,mcAddr,mcPort,mdbAddr,mdbPort,mdrAddr,mdrPort);
    }
}
