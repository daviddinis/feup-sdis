package peers;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class PeerService {

    private String serverId;
    private String protocolVersion;
    private String serviceAccessPoint;

    private PeerChannel multiChannel;
    private PeerChannel multiDataBackUpChannel;
    private PeerChannel multiDataRestoreChannel;

    private InitiatorPeer initiatorPeer;

    public PeerService(String serverId,String protocolVersion, String serviceAccessPoint,InetAddress mcAddr,int mcPort,InetAddress mdbAddr,int mdbPort,
                       InetAddress mdrAddr,int mdrPort) throws IOException {

        this.serverId = serverId;
        this.protocolVersion = protocolVersion;
        this.serviceAccessPoint = serviceAccessPoint;

        multiChannel = new PeerChannel(mcAddr,mcPort);
        multiDataBackUpChannel = new PeerChannel(mdbAddr, mdbPort);
        multiDataRestoreChannel = new PeerChannel(mdrAddr,mdrPort);

        System.out.println("Multicast channel addr: "+ mcAddr+" port: "+ mcPort);
        System.out.println("Multicast data backup addr: "+ mdbAddr+" port: "+ mdbPort);
        System.out.println("Multicast data restore addr: "+ mdrAddr+" port: "+ mdrPort);

        initiatorPeer = new InitiatorPeer(multiChannel, multiDataBackUpChannel,multiDataRestoreChannel,this);

        try{
            //TODO add ip address
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(this.serviceAccessPoint,initiatorPeer);
        }catch (Exception e){
            //TODO add rebind
            System.out.println("Peer error: "+ e.getMessage());
            e.printStackTrace();
        }

        multiChannel.receiveMessage();
        multiDataBackUpChannel.receiveMessage();
        multiDataRestoreChannel.receiveMessage();

    }

    public String getServerId() {
        return serverId;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }
}
