package peers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.nio.file.NoSuchFileException;
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

    private String serverId;
    private String protocolVersion;
    private String serviceAccessPoint;

    private PeerChannel controlChannel;
    private PeerChannel dataBackupChannel;
    private PeerChannel dataRestoreChannel;

    private String myFilesPath;
    private String chunksPath;

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
     *  registers the chunk number of the stored chunks
     *  key = file_id
     *  value = array with the chunk numbers of the stored chunks
     */
    private ConcurrentHashMap<String,ArrayList<Integer>> storedChunks;

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

        chunkMap = new ConcurrentHashMap<>();
        fileReplicationDegrees = new ConcurrentHashMap<>();
        storedChunks = new ConcurrentHashMap<>();

    }

    /**
     * Create a directory
     * @param path path of the directory to be created
     */
    public void createDir(String path) {

        File file = new File(path);

        if(file.mkdir()){
            System.out.println("Directory: " + path + " created");
        }
    }

    /**
     * Constructs a message header with the given fields
     * @param fields header fields
     * @return message header
     */
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
     * with the desired replication degree
     * checks if the chunk is already stored, if not, it is stored
     *
     * @param fileID file ID of the file the chunk belongs to
     * @param chunkNo chunk number of the chunk to register
     * @param replicationDegree desired replication degree of the chunk
     * @return true if the chunk was registered, false otherwise
     */
    private boolean registerChunk(String fileID, int chunkNo, int replicationDegree){
        ArrayList<Integer> fileChunks = storedChunks.get(fileID);

        // no chunks registered for this file, register the file and register the chunk
        if (fileChunks == null){
            registerFile(fileID, replicationDegree);
            fileChunks = new ArrayList<>();
            fileChunks.add(chunkNo);
            storedChunks.put(fileID,fileChunks);
            return true;
        }

        // check if the chunk was already registered
        for (Integer storedChunkNo : fileChunks) {
            if (storedChunkNo == chunkNo)
                return false;
        }

        fileChunks.add(chunkNo);
        return true;
    }

    /**
     * Get the perceived replication degree of a chunk
     * Checks the peer's chunk map to see how many peers have a copy of the chunk
     * @param fileID file ID of the file the chunk belongs to
     * @param chunkNo chunk number of the chunk
     * @return the perceived replication degree of the chunk
     */
    private int getReplicationDegree(String fileID, String chunkNo){
        ArrayList chunkPeers = chunkMap.get(fileID+'_'+chunkNo);
        if(chunkPeers == null)
            return -1;
        else return chunkPeers.size();
    }

    /**
     * Chunk Backup Subprotocol
     * Launches a Thread which constructs and sends a PUTCHUNK message for a given chunk
     * After sending the message, it waits, at first, one second, and checks if the
     * desired replication degree was reached for that chunk
     * if not, it doubles the waiting interval and tries again,
     * up to a maximum of five times
     * @param fileId file ID for the file that the chunk belongs to
     * @param chunkNo number of the chunk to be backed up
     * @param replicationDegree desired replication degree of the chunk
     * @param chunk chunk data
     */
    public void requestChunkBackup(String fileId, int chunkNo, int replicationDegree, byte[] chunk){

        Runnable task = () -> {
            int counter = 1, multiplier = 1;
            String header = makeHeader("PUTCHUNK", protocolVersion, serverId, fileId,
                    Integer.toString(chunkNo), Integer.toString(replicationDegree));

            byte[] headerBytes = header.getBytes();

            //concatenate contents of header and body
            byte[] buf = new byte[headerBytes.length + chunk.length];
            System.arraycopy(headerBytes, 0, buf, 0, headerBytes.length);
            System.arraycopy(chunk, 0, buf, headerBytes.length, chunk.length);

            do {
                try {
                    dataBackupChannel.sendMessage(buf);
                    printHeader(header,true);
                } catch (IOException e) {
                    System.err.println("IOException :: PeerService :: Failed to send PUTCHUNK message");

                    counter++;
                    multiplier *= 2;

                    if(counter > 5)
                        break;
                    else continue;
                }

                // wait and process response
                try {
                    Thread.sleep(1000 * multiplier);
                } catch (InterruptedException e) {
                    System.err.println("InterruptedException :: PeerService :: Retrying");
                    continue;
                }
                counter++;
                multiplier *= 2;
            } while(counter <= 5 && getReplicationDegree(fileId,Integer.toString(chunkNo)) < replicationDegree);

            int achievedRepDeg = getReplicationDegree(fileId,Integer.toString(chunkNo));
            if(counter > 5) {
                System.out.println("Timed out!");
                System.out.format("Achieved replication degree: %d", achievedRepDeg);
            }
            else if(achievedRepDeg >= replicationDegree){
                System.out.println("Success!");
            }
        };
        new Thread(task).start();
    }

    /**
     * Called when a message is received,
     * checks the instruction and calls the appropriate protocol
     * @param message received message
     */
    public void messageHandler(byte[] message){
        String data = new String(message, 0, message.length);
        data = data.trim();
        String[] dataPieces = data.split(CRLF+CRLF);
        String messageHeader[] = dataPieces[0].split(" ");

        //check message type
        String messageType = messageHeader[0];
        String protocolVersion = messageHeader[1];

        String senderID = messageHeader[2];
        if (senderID.equals(this.serverId))// backup request sent from this peer, ignore
            return;

        switch (messageType){
            case "PUTCHUNK": {
                if(messageHeader.length < 6){
                    System.err.println("Not enough fields on header for PUTCHUNK");
                    break;
                }
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
                printHeader(dataPieces[0],false);
                String fileID = messageHeader[3];
                String chunkNo = messageHeader[4];
                registerStorage(protocolVersion,senderID,fileID,chunkNo);
                break;
            }
            case "DELETE": {
                if(messageHeader.length < 4){
                    System.err.println("Not enough fields on header for DELETE");
                    break;
                }
                printHeader(dataPieces[0],false);
                String fileID = messageHeader[3];
                deleteFile(fileID);
                break;
            }
            default: {
                System.out.format("Unrecognized operation: %s", messageType);
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
     * @param chunkNo chunk number of the chunk to be stored
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
            printHeader(response,true);
        } catch (IOException e) {
            System.err.println("IOException :: PeerService :: Unable to backup chunk.");
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
     * @param protocolVersion protocol version used by the message sender
     * @param senderID id of the sender of the STORED message
     * @param fileID id of the file whose chunk was stored
     * @param chunkNo chunk number of the stored chunk
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
            for (Integer chunkPeer : chunkPeers) {
                if (chunkPeer == sender)    // peer was already registered
                    return true;
            }
            chunkPeers.add(sender);
        }

        return true;
    }

    /**
     * Constructs and sends a DELETE message for a given file, identified
     * by its file ID
     * @param fileID file ID of the file to be deleted
     */
    public void requestFileDeletion(String fileID){
        String message = makeHeader("DELETE",protocolVersion,serverId,fileID);
        try {
            controlChannel.sendMessage(message.getBytes());
            printHeader(message,true);
        }catch (IOException e){
            System.err.println("IOException :: PeerService :: Failed to send DELETE message.");
        }
    }

    /**
     * Deletes the stored chunks (if any) the peer has
     * belonging to file identified by the parameter file ID
     * @param fileID file ID of the file whose chunks are to be deleted
     */
    public void deleteFile(String fileID){
        ArrayList<Integer> fileChunks = storedChunks.get(fileID);
        if(fileChunks == null) {  // peer has no chunks belonging to this file
            System.out.format("This peer has no chunks belonging to the file with file ID %s", fileID);
            return;
        }

        for (Integer fileChunk : fileChunks) {
            String chunkPath = chunksPath + '/' + fileID + '_' + Integer.toString(fileChunk);
            File chunk = new File(chunkPath);
            if(chunk.delete()){
                System.out.format("Chunk %d, belonging to file %s deleted", fileChunk, fileID);
            }
        }
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
}
