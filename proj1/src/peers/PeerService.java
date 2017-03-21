package peers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

    private PeerChannel controlChannel;
    private PeerChannel dataBackupChannel;
    private PeerChannel dataRestoreChannel;

    private String myFilesPath;
    private String chunksPath;

    private PeerClientLink initiatorPeer;

    public PeerService(String serverId,String protocolVersion, String serviceAccessPoint,InetAddress mcAddr,int mcPort,InetAddress mdbAddr,int mdbPort,
                       InetAddress mdrAddr,int mdrPort) throws IOException {

        this.serverId = serverId;
        this.protocolVersion = protocolVersion;
        this.serviceAccessPoint = serviceAccessPoint;

        controlChannel = new PeerChannel(mcAddr,mcPort,this);
        System.out.println("Control Channel ready! Listening...");
        dataBackupChannel = new PeerChannel(mdbAddr, mdbPort,this);
        System.out.println("Data Backup Channel ready! Listening...");
        dataRestoreChannel = new PeerChannel(mdrAddr,mdrPort,this);
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


        chunksPath = serverId + "/chunks";
        myFilesPath = serverId + "/my_files";

        createDir(serverId);
        createDir(myFilesPath);
        createDir(chunksPath);

        controlChannel.receiveMessage();
        dataBackupChannel.receiveMessage();
        dataRestoreChannel.receiveMessage();

    }

    public void createDir(String folderPath) {

        File file = new File(folderPath);

        if(file.mkdir()){
            System.out.println("Directory: " + folderPath + " created");
        }

    }

    private String makeHeader(String... fields) {

        String header = "";

        for(String field : fields){
            header = header.concat(field+" ");
        }

        header = header.concat(CRLF + CRLF);

        return header;
    }

    public void requestChunkBackup(String fileId, int chunkNo, int replicationDegree, byte[] chunk) throws IOException {

        String header = makeHeader("PUTCHUNK",protocolVersion,serverId,fileId,
                Integer.toString(chunkNo),Integer.toString(replicationDegree));

        byte[] headerBytes = header.getBytes();
        byte[] buf = new byte[headerBytes.length + chunk.length];
        System.arraycopy(headerBytes,0,buf,0,headerBytes.length);           //concatenate contents of header and body
        System.arraycopy(chunk,0,buf,headerBytes.length,chunk.length);

        dataBackupChannel.sendMessage(buf);
    }

    public void messageHandler(byte[] buffer){
        String data = new String(buffer, 0, buffer.length);
        data = data.trim();
        String[] dataPieces = data.split(CRLF+CRLF);
        String messageHeader[] = dataPieces[0].split(" ");

        //check message type
        String messageType = messageHeader[0];
        String protocolVersion = messageHeader[1];

        switch (messageType){
            case "PUTCHUNK":
                String senderID = messageHeader[2];
                if(senderID.equals(this.serverId))  // backup request sent from this peer
                    break;                          // ignore
                String fileID = messageHeader[3];
                String chunkNo = messageHeader[4];
                String replicationDegree = messageHeader[5];
                String chunk = dataPieces[1];
                backupChunk(protocolVersion, fileID,chunkNo,replicationDegree,chunk);
                break;
            default:
                //todo treat this??
                break;
        }
    }

    private boolean backupChunk(String protocolVersion, String fileID, String chunkNo, String replicationDegree, String chunk){
        byte[] chunkData = chunk.getBytes();
        try {
            FileOutputStream chunkFile  = new FileOutputStream(chunksPath + "/" + fileID + "_" + chunkNo);
            chunkFile.write(chunkData);
            String response = makeHeader("STORED",protocolVersion,serverId,fileID,chunkNo);
            controlChannel.sendMessage(response.getBytes());
        } catch (IOException e) {
            System.err.println("chunk backup subprotocol :: Unable to backup chunk.");
            return false;
        }
        return true;
    }
}
