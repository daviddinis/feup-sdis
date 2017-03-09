package src.peers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class PeerLauncher {
    public static void main(String[] args) throws IOException {
        if(args.length != 9){
            throw new IllegalArgumentException("\nUsage: java PeerLauncher <peerId> <protocolVersion> <accessPoint>" +
                    " <mcAddr> <mcPort> <mdbSddr> <mdbPort> <mdrAddr> <mdrPort>");
        }

        String serverId = args[0];
        String protocolVersion = args[1];
        String accessPoint = args[2];
        InetAddress mcAddr = InetAddress.getByName(args[3]);
        int mcPort = Integer.parseInt(args[4]);
        InetAddress mdbAddr = InetAddress.getByName(args[5]);
        int mdbPort = Integer.parseInt(args[6]);
        InetAddress mdrAddr = InetAddress.getByName(args[7]);
        int mdrPort = Integer.parseInt(args[8]);

        PeerService peerService = new PeerService(serverId, protocolVersion, accessPoint,mcAddr,mcPort,
                mdbAddr,mdbPort,mdrAddr,mdrPort);
    }
}
