package peers;

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class PeerService {

    public static final String CRLF = "\r\n";
    public static final int CHUNK_SIZE = 64000;
    public static final int ERROR = -1;

    private String serverId;
    private String protocolVersion;
    private String serviceAccessPoint;

    private PeerChannel controlChannel;
    private PeerChannel dataBackupChannel;
    private PeerChannel dataRestoreChannel;

    private String myFilesPath;
    private String chunksPath;
    private String restoredFilesPath;

    private PeerClientLink initiatorPeer;

    /**
     * registers the peers that have stored chunks
     * key = <fileID>_<ChunkNo>
     * value = array with the peer id of the peers that have stored that chunk
     *
     */
    private ConcurrentHashMap<String,ArrayList<Integer>> chunkMap;

    /**
     * stores the desired replication degree for every file the
     * peer has stored or has chunks of
     */
    private ConcurrentHashMap<String,Integer> fileReplicationDegrees;

    /**
     * stores the number of chunks every file has
     */
    private ConcurrentHashMap<String,Integer> fileChunkNum;

    /**
     *  registers the chunk number of the stored chunks
     *  key = file_id
     *  value = array with the chunk numbers of the stored chunks
     */
    private ConcurrentHashMap<String,ArrayList<Integer>> storedChunks;

    /**
     * registers if a file was already restored
     * key = file_id
     * value = a boolean that is true when a file was already restored and false otherwise
     */
    private ConcurrentHashMap<String,Boolean> restoredChunks;

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
        restoredFilesPath = serverId + "/restored_files";

        createDir(serverId);
        createDir(myFilesPath);
        createDir(chunksPath);
        createDir(restoredFilesPath);

        controlChannel.receiveMessage();
        dataBackupChannel.receiveMessage();
        dataRestoreChannel.receiveMessage();

        chunkMap = new ConcurrentHashMap<>();
        fileReplicationDegrees = new ConcurrentHashMap<>();
        fileChunkNum = new ConcurrentHashMap<>();
        storedChunks = new ConcurrentHashMap<>();
        restoredChunks = new ConcurrentHashMap<>();
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

    /**
     * Places a file in the replication degree hash map and
     * initializes a list in the peer list
     * @param fileID file ID for the sent file
     * @param replicationDegree desired replication degree for the file
     */
    public void registerFile(String fileID, int replicationDegree){
        if(fileReplicationDegrees.get(fileID) != null){
            fileReplicationDegrees.remove(fileID);
        }

        fileReplicationDegrees.put(fileID,replicationDegree);
    }

    /**
     * Registers a chunk in the storedChunks HashMap
     * checks if the file is already registered, if not, it is registered
     * checks if the chunk is already stored, if not, it is stored
     *
     * @param fileID
     * @param chunkNo
     * @param replicationDegree
     * @return
     */
    private boolean registerChunk(String fileID, int chunkNo, int replicationDegree){
        ArrayList<Integer> fileChunks = storedChunks.get(fileID);
        if (fileChunks == null){ // no chunks registered for this file
            registerFile(fileID, replicationDegree);
            fileChunks = new ArrayList<>();
            fileChunks.add(chunkNo);
            storedChunks.put(fileID,fileChunks);
            return true;
        }
        for (Integer fileChunkNo : fileChunks) {
            if (fileChunkNo == chunkNo)
                return false;
        }
        fileChunks.add(chunkNo);
        return true;
    }

    public boolean registerNumChunks(String fileID, int numChunks){
        if(fileChunkNum.get(fileID) != null)
            return false;

        fileChunkNum.put(fileID,numChunks);
        return true;
    }

    public int getNumChunks(String fileID){

        Object ChunksNo = fileChunkNum.get(fileID);

        if(ChunksNo == null)
            return ERROR;

        return (int)ChunksNo;
    }

    private int getReplicationDegree(String fileID, String chunkNo){
        ArrayList chunkPeers = chunkMap.get(fileID+'_'+chunkNo);
        if(chunkPeers == null)
            return -1;
        else return chunkPeers.size();
    }

    public void requestChunkBackup(String fileId, int chunkNo, int replicationDegree, byte[] chunk) throws IOException {

        Runnable task = () -> {
            int counter = 1;

            do {
                String header = makeHeader("PUTCHUNK", protocolVersion, serverId, fileId,
                        Integer.toString(chunkNo), Integer.toString(replicationDegree));

                byte[] headerBytes = header.getBytes();
                byte[] buf = new byte[headerBytes.length + chunk.length];
                System.arraycopy(headerBytes, 0, buf, 0, headerBytes.length);           //concatenate contents of header and body
                System.arraycopy(chunk, 0, buf, headerBytes.length, chunk.length);

                try {
                    dataBackupChannel.sendMessage(buf);
                } catch (IOException e) {
                    //TODO treat
                }

                // wait and process response
                try {
                    Thread.sleep(1000 * counter);
                } catch (InterruptedException e) {
                    //TODO treat
                }
                counter++;
            } while(counter <= 5 && getReplicationDegree(fileId,Integer.toString(chunkNo)) < replicationDegree);
            if(counter > 5) {
                System.out.println("Timed out!");
                System.out.println(getReplicationDegree(fileId,Integer.toString(chunkNo)));
            }
            else{
                System.out.println("Success!");
            }
        };
        new Thread(task).start();
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
            case "PUTCHUNK": {
                if(messageHeader.length < 6){
                    System.out.println(messageHeader.length);
                    System.err.println("Not enough fields on header for PUTCHUNK");
                    break;
                }
                String senderID = messageHeader[2];
                if (senderID.equals(this.serverId))  // backup request sent from this peer
                    break;                           // ignore
                printHeader(dataPieces[0], false);
                String fileID = messageHeader[3];
                String chunkNo = messageHeader[4];
                String replicationDegree = messageHeader[5];
                String chunk = dataPieces[1];
                storeChunk(protocolVersion, fileID, chunkNo, replicationDegree, chunk);
                break;
            }
            case "STORED": {
                if(messageHeader.length < 5){
                    System.err.println("Not enough fields on header for STORED");
                    break;
                }
                String senderID = messageHeader[2];
                if (senderID.equals(this.serverId))  // message sent from this peer
                    break;
                printHeader(dataPieces[0],false);
                String fileID = messageHeader[3];
                String chunkNo = messageHeader[4];
                registerStorage(protocolVersion,senderID,fileID,chunkNo);
                break;
            }
            case "GETCHUNK": {
                if(messageHeader.length < 5){
                    System.err.println("Not enough fields on header for GETCHUNK");
                    break;
                }
                String senderID = messageHeader[2];
            }
            default: {
                //todo treat this??
                break;
            }
        }
    }

    /**
     * Function called when the peer receives a PUTCHUNK message from another peer
     * registers the file and the chunk
     *
     * @param protocolVersion version of the Chunk Backup Subprotocol
     * @param fileID file ID of the file the chunk belongs to
     * @param chunkNo
     * @param replicationDegree desired file replication degree
     * @param chunk chunk data
     * @return true if the chunk was registered and stored
     */
    private boolean storeChunk(String protocolVersion, String fileID, String chunkNo, String replicationDegree, String chunk){
        if(protocolVersion == null || fileID == null || chunk == null
                || replicationDegree == null || replicationDegree == null
                || chunkNo == null || chunk == null)
            return false;

        byte[] chunkData = chunk.getBytes();
        try {
            // Check if the chunk is already stored
            if(registerChunk(fileID,Integer.parseInt(chunkNo), Integer.parseInt(replicationDegree))) {
                String filename = fileID + "_" + chunkNo;
                FileOutputStream chunkFile = new FileOutputStream(chunksPath + "/" + filename);
                chunkFile.write(chunkData);
            }
            Random random = new Random(System.currentTimeMillis());
            long waitTime = random.nextInt(400);
            Thread.sleep(waitTime);
            String response = makeHeader("STORED",protocolVersion,serverId,fileID,chunkNo);
            registerStorage(protocolVersion,this.serverId,fileID,chunkNo);
            controlChannel.sendMessage(response.getBytes());
        } catch (IOException e) {
            System.err.println("chunk backup subprotocol :: Unable to backup chunk.");
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Called when a peer receives a STORED message from another peer
     * It updates the peer's chunkMap to reflect the perceived
     * replication degree of the chunk
     * @param protocolVersion
     * @param senderID
     * @param fileID
     * @param chunkNo
     */
    private boolean registerStorage(String protocolVersion, String senderID, String fileID, String chunkNo){
        if(protocolVersion == null || senderID == null || fileID == null || chunkNo == null)
            return false;

        ArrayList<Integer> chunkPeers = chunkMap.get(fileID+'_'+chunkNo);
        int sender = Integer.parseInt(senderID);
        if(chunkPeers == null){
            chunkPeers = new ArrayList<>();
            chunkPeers.add(sender);
            chunkMap.put(fileID+'_'+chunkNo,chunkPeers);
        } else {
            for (int i = 0; i < chunkPeers.size() ; i++) {
                if(chunkPeers.get(i) == sender) {   // peer was already registered
                    System.out.println("Here");
                    return true;
                }
            }
            chunkPeers.add(sender);
        }

        return true;
    }

    /**
     * Prints the header fields
     * @param header the header string
     * @param sent true if the message is being sent
     */
    private void printHeader(String header, boolean sent){
        System.out.println("Message " + (sent ? "sent" : "received"));
        System.out.println(header);
    }

    /**
     * Creates the message "GETCHUNK" and send its
     * @param fileId id of the file to be restored
     * @param chunkNo Chunk number
     */
    public void requestChunkRestore(String fileId, int chunkNo) {

        Runnable task = () -> {

            String header = makeHeader("GETCHUNK", protocolVersion, serverId, fileId,
                    Integer.toString(chunkNo));

            byte[] headerBytes = header.getBytes();

            try {
                controlChannel.sendMessage(headerBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

        };
        new Thread(task).start();
    }

    /**
     * Adds a file_id to the restore hash map
     * @param fileId id of the file to be added
     * @return true if the file was added false otherwise
     */
    public boolean addToRestoredHashMap(String fileId){

        if (restoredChunks.get(fileId) != null) {
            return false;
        }

        restoredChunks.put(fileId,false);

        return true;
    }
}
