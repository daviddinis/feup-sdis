package peers;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class PeerService {

    public static final String CRLF = "\r\n";
    public static final int CHUNK_SIZE = 64000;

    private String serverId;
    private String protocolVersion;
    private String serviceAccessPoint;

    private PeerChannel multiChannel;
    private PeerChannel multiDataBackUpChannel;
    private PeerChannel multiDataRestoreChannel;

    private PeerClientLink initiatorPeer;

    public PeerService(String serverId,String protocolVersion, String serviceAccessPoint,InetAddress mcAddr,int mcPort,InetAddress mdbAddr,int mdbPort,
                       InetAddress mdrAddr,int mdrPort) throws IOException {

        this.serverId = serverId;
        this.protocolVersion = protocolVersion;
        this.serviceAccessPoint = serviceAccessPoint;

        multiChannel = new PeerChannel(mcAddr,mcPort);
        System.out.println("Control Channel ready! Listening...");
        multiDataBackUpChannel = new PeerChannel(mdbAddr, mdbPort);
        System.out.println("Data Backup Channel ready! Listening...");
        multiDataRestoreChannel = new PeerChannel(mdrAddr,mdrPort);
        System.out.println("Restore Channel ready! Listening...");

        System.out.println("Multicast channel addr: "+ mcAddr+" port: "+ mcPort);
        System.out.println("Multicast data backup addr: "+ mdbAddr+" port: "+ mdbPort);
        System.out.println("Multicast data restore addr: "+ mdrAddr+" port: "+ mdrPort);

        initiatorPeer = new PeerClientLink(this);

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

    private String getHeader(String... fields) {

        String header = "";

        for(String field : fields){
            header = header.concat(field+" ");
        }

        header.concat(CRLF + CRLF);

        return header;
    }

    public void requestChunkBackup(String pathname, String replicationDegree,byte[] chunk) throws IOException {

        String header = getHeader("PUTCHUNK",protocolVersion,pathname,
                "ChunkNO",replicationDegree);

        System.out.println(header);

        byte[] buf = header.getBytes();

        multiDataBackUpChannel.sendMessage(buf);

    }
}
