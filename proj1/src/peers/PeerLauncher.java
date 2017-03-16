package peers;

import java.io.IOException;
import java.net.InetAddress;

public class PeerLauncher {
    public static void main(String[] args) throws IOException {
        if(args.length != 9){
            System.out.println(args.length);
            throw new IllegalArgumentException("\nUsage: java PeerLauncher <peerId> <protocolVersion> <accessPoint>" +
                    " <mcAddr> <mcPort> <mdbSddr> <mdbPort> <mdrAddr> <mdrPort>");
        }

        String serverId = args[0];
        String protocolVersion = args[1];
        String serviceAccessPoint = args[2];

        InetAddress mcAddr = InetAddress.getByName(args[3]);
        int mcPort = Integer.parseInt(args[4]);

        InetAddress mdbAddr = InetAddress.getByName(args[5]);
        int mdbPort = Integer.parseInt(args[6]);

        InetAddress mdrAddr = InetAddress.getByName(args[7]);
        int mdrPort = Integer.parseInt(args[8]);

        PeerService peerService = new PeerService(serverId, protocolVersion, serviceAccessPoint,mcAddr,mcPort,
                mdbAddr,mdbPort,mdrAddr,mdrPort);
    }
}
