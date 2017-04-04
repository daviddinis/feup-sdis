package peers;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by epassos on 3/29/17.
 */
public class ChunkManager {

    /**
     * stores the number of chunks every file has
     */
    private final ConcurrentHashMap<String, Integer> numChunksFile;

    /**
     * registers the peers that have stored chunks
     * key = <fileID>_<ChunkNo>
     * value = array with the peer id of the peers that have stored that chunk
     */
    private final ConcurrentHashMap<String, ArrayList<Integer>> chunkMap;

    /**
     * stores the desired replication degree for every file the
     * peer has stored or has chunks of
     */
    private final ConcurrentHashMap<String, Integer> fileReplicationDegrees;

    /**
     * registers the chunk number of the stored chunks
     * key = <fileID>
     * value = array with the chunk numbers of the stored chunks
     */
    private final ConcurrentHashMap<String, ArrayList<Integer>> storedChunks;

    private final String chunksPath;
    private final String serverId;

    public ChunkManager(String serverId, String chunksPath) {

        this.serverId = serverId;
        this.chunksPath = chunksPath;
        fileReplicationDegrees = new ConcurrentHashMap<>();
        storedChunks = new ConcurrentHashMap<>();
        chunkMap = new ConcurrentHashMap<>();
        numChunksFile = new ConcurrentHashMap<>();
    }

    /**
     * Places a file in the replication degree hash map and
     * initializes a list in the peer list
     *
     * @param fileID            file ID for the sent file
     * @param replicationDegree desired replication degree for the file
     */
    public void registerFile(String fileID, int replicationDegree) {
        if (fileReplicationDegrees.get(fileID) != null) {
            fileReplicationDegrees.remove(fileID);
        }
        fileReplicationDegrees.put(fileID, replicationDegree);
    }

    /**
     * Registers a chunk in the storedChunks HashMap
     * checks if the file is already registered, if not, it is registered
     * with the desired replication degree
     * checks if the chunk is already stored, if not, it is stored
     *
     * @param fileID            file ID of the file the chunk belongs to
     * @param chunkNo           chunk number of the chunk to register
     * @param replicationDegree desired replication degree of the chunk
     */
    private void registerChunk(String fileID, int chunkNo, int replicationDegree) {
        ArrayList<Integer> fileChunks = storedChunks.get(fileID);

        if (hasChunk(fileID, chunkNo))
            return;

        // no chunks registered for this file, register the file and register the chunk
        if (fileChunks == null) {
            registerFile(fileID, replicationDegree);
            fileChunks = new ArrayList<>();
            fileChunks.add(chunkNo);
            storedChunks.put(fileID, fileChunks);
            return;
        }
        fileChunks.add(chunkNo);
    }

    /**
     * Function called when the peer receives a PUTCHUNK message from another peer
     * registers the file and the chunk
     *
     * @param protocolVersion   version of the Chunk Backup Subprotocol
     * @param fileID            file ID of the file the chunk belongs to
     * @param chunkNo           chunk number of the chunk to be stored
     * @param replicationDegree desired file replication degree
     * @param chunkData         chunk data
     * @return true if the chunk was registered and stored
     */
    public boolean storeChunk(String protocolVersion, String fileID, String chunkNo, String replicationDegree, byte[] chunkData) {
        if (protocolVersion == null || fileID == null || chunkData == null
                || replicationDegree == null
                || chunkNo == null)
            return false;

        try {
            // Check if the chunk is already stored
            if (!hasChunk(fileID, Integer.parseInt(chunkNo))) {
                registerChunk(fileID, Integer.parseInt(chunkNo), Integer.parseInt(replicationDegree));
                String filename = fileID + "_" + chunkNo;
                FileOutputStream chunkFile = new FileOutputStream(chunksPath + "/" + filename);
                chunkFile.write(chunkData);
            }
            Random random = new Random(System.currentTimeMillis());
            long waitTime = random.nextInt(400);
            Thread.sleep(waitTime);
        } catch (IOException e) {
            System.err.println("IOException :: PeerService :: Unable to backup chunk.");
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return registerStorage(protocolVersion, this.serverId, fileID, chunkNo);
    }

    /**
     * Called when a peer receives a STORED message from another peer
     * It updates the peer's chunkMap to reflect the perceived
     * replication degree of the chunk
     *
     * @param protocolVersion protocol version used by the message sender
     * @param senderID        id of the sender of the STORED message
     * @param fileID          id of the file whose chunk was stored
     * @param chunkNo         chunk number of the stored chunk
     */
    public boolean registerStorage(String protocolVersion, String senderID, String fileID, String chunkNo) {
        if (protocolVersion == null || senderID == null || fileID == null || chunkNo == null)
            return false;

        ArrayList<Integer> chunkPeers = chunkMap.get(fileID + '_' + chunkNo);
        int sender = Integer.parseInt(senderID);
        if (chunkPeers == null) {
            chunkPeers = new ArrayList<>();
            chunkPeers.add(sender);
            chunkMap.put(fileID + '_' + chunkNo, chunkPeers);
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
     * Verifies if a given chunk of a given file is stored on the peer
     *
     * @param fileID  id of the file
     * @param chunkNo Number of the chunk to be searched
     * @return true if the chunk exists on the filesystem, false otherwise
     */
    public boolean hasChunk(String fileID, Integer chunkNo) {

        ArrayList<Integer> fileStoredChunks;
        fileStoredChunks = storedChunks.get(fileID);

        return fileStoredChunks != null && fileStoredChunks.contains(chunkNo);
    }

    /**
     * Get the perceived replication degree of a chunk
     * Checks the peer's chunk map to see how many peers have a copy of the chunk
     *
     * @param fileID  file ID of the file the chunk belongs to
     * @param chunkNo chunk number of the chunk
     * @return the perceived replication degree of the chunk
     */
    public int getReplicationDegree(String fileID, String chunkNo) {
        String key = fileID + '_' + chunkNo;
        return chunkMap.containsKey(key) ? chunkMap.get(key).size() : -1;
    }

    public boolean registerNumChunks(String fileID, int numChunks) {
        if (numChunksFile.containsKey(fileID))
            return false;

        numChunksFile.put(fileID, numChunks);
        return true;
    }

    /**
     * Returns the number of chunks a file has
     *
     * @param fileID file ID of the file
     * @return number of chunks the file has or ERROR (-1) if the file is not registered
     */
    public int getNumChunks(String fileID) {
        return numChunksFile.getOrDefault(fileID, PeerService.ERROR);
    }

    /**
     * Deletes the stored chunks (if any) the peer has
     * belonging to file identified by the parameter file ID
     *
     * @param fileID file ID of the file whose chunks are to be deleted
     */
    public void deleteFile(String fileID) {
        ArrayList<Integer> fileChunks = storedChunks.get(fileID);
        if (fileChunks == null) {  // peer has no chunks belonging to this file
            System.out.format("This peer has no chunks belonging to the file with file ID %s", fileID);
            return;
        }

        for (Integer fileChunk : fileChunks) {
            String chunkName = fileID + '_' + Integer.toString(fileChunk);

            chunkMap.remove(chunkName);
            fileReplicationDegrees.remove(fileID);

            String chunkPath = chunksPath + '/' + chunkName;
            File chunk = new File(chunkPath);
            if (chunk.delete())
                System.out.format("Chunk %d, belonging to file %s deleted", fileChunk, fileID);
        }

        storedChunks.remove(fileID);
    }

    public byte[] getChunkData(String fileID, String chunkNo) throws IOException {

        String filename = fileID + "_" + chunkNo;
        FileInputStream chunkFile;

        chunkFile = new FileInputStream(chunksPath + "/" + filename);

        byte[] chunkData;
        int readableBytes = chunkFile.available();

        if (readableBytes > PeerService.CHUNK_SIZE)
            chunkData = new byte[PeerService.CHUNK_SIZE];
        else
            chunkData = new byte[readableBytes];


        chunkFile.read(chunkData);

        return chunkData;
    }
}
