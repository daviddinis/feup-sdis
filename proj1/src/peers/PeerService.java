package peers;

import java.io.File;
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

        multiChannel = new PeerChannel(mcAddr,mcPort,this);
        System.out.println("Control Channel ready! Listening...");
        multiDataBackUpChannel = new PeerChannel(mdbAddr, mdbPort,this);
        System.out.println("Data Backup Channel ready! Listening...");
        multiDataRestoreChannel = new PeerChannel(mdrAddr,mdrPort,this);
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

        creatingAFolder(serverId);
        creatingAFolder(serverId + "/MyFiles");
        creatingAFolder(serverId + "/PeersFiles");

        multiChannel.receiveMessage();
        multiDataBackUpChannel.receiveMessage();
        multiDataRestoreChannel.receiveMessage();

    }

    public void creatingAFolder(String folderPath) {

        File file = new File(folderPath);
        boolean success = file.mkdir();

        if(success){
            System.out.println("Directory: " + folderPath + " created");
        }

    }

    private String getHeader(String... fields) {

        String header = "";

        for(String field : fields){
            header = header.concat(field+" ");
        }

        header.concat(CRLF + CRLF);

        return header;
    }

    public void requestChunkBackup(String fileId, String replicationDegree,byte[] chunk) throws IOException {

        String header = getHeader("PUTCHUNK",protocolVersion,serverId,fileId,
                "ChunkNO",replicationDegree);

        System.out.println(header);

        byte[] buf = header.getBytes();

        multiDataBackUpChannel.sendMessage(buf);

    }

    public void messageReceiverHandler(byte[] data){

        // Message Handler............
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
